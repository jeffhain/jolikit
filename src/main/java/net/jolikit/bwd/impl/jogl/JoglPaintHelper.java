/*
 * Copyright 2019-2024 Jeff Hain
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.jolikit.bwd.impl.jogl;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GL3ES3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLProfile;

import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.utils.basics.BindingError;
import net.jolikit.bwd.impl.utils.basics.ScaleHelper;
import net.jolikit.bwd.impl.utils.graphics.BindingColorUtils;
import net.jolikit.bwd.impl.utils.graphics.DirectBuffers;
import net.jolikit.bwd.impl.utils.graphics.IntArrayGraphicBuffer;
import net.jolikit.bwd.impl.utils.graphics.InterfaceColorTypeHelper;
import net.jolikit.bwd.impl.utils.graphics.PremulNativeRgbaHelper;
import net.jolikit.lang.LangUtils;
import net.jolikit.lang.NbrsUtils;

public class JoglPaintHelper {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    /**
     * Either works.
     * If true, using glBindAttribLocation(...),
     * else, using glGetAttribLocation(...).
     */
    private static final boolean MUST_BIND_BEFORE_PROG_CREATION = false;
    
    /**
     * TODO jogl When resizing the window, for example by dragging its border,
     * if client area height becomes 0, and we use glTexImage2D(...), then,
     * once mouse button is released (to stop the border drag),
     * the following exception is thrown:
     * Caused by: java.lang.IndexOutOfBoundsException: Required 1272 remaining bytes in buffer, only had 0
     *     at com.jogamp.common.nio.Buffers.rangeCheckBytes(Buffers.java:1056)
     *     at jogamp.opengl.gl4.GL4bcImpl.glTexImage2D(GL4bcImpl.java:5519)
     * 
     * This is due to  GLBuffers.sizeof(...) returning a value > 0 when height is 0,
     * because internally it uses max(1,height) (which is a bit weird).
     * As a result, we take care not to use glTexImage2D(...) when height,
     * or width, is 0.
     * NB: There is no such issue with LWJGL.
     */
    private static final boolean MUST_NOT_RENDER_IF_EMPTY_AREA = true;
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private static class MyTextureData {
        final IntBuffer texturePixelsBuffer;
        final int texturePixelsScanlineStride;
        /**
         * Rectangle in client area, from which GL vertices coordinates
         * are computed.
         */
        final GRect textureRect;
        public MyTextureData(
            IntBuffer texturePixelsBuffer,
            int texturePixelsScanlineStride,
            GRect textureRect) {
            this.texturePixelsBuffer = texturePixelsBuffer;
            this.texturePixelsScanlineStride = texturePixelsScanlineStride;
            this.textureRect = textureRect;
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /*
     * Not available in Java 6.
     */
    
    private static final int Float_BYTES = 4;
    private static final int Integer_BYTES = 4;
    
    /*
     * TODO jogl Need buffers to be direct, else doesn't work (but silently!!!).
     * 
     * Making sure native buffers are just created once and for all,
     * for there is no public API to clear them explicitly.
     */
    
    private static final float[] TEXTURE_COORD_ARR = new float[]{
        0.0f, 0.0f,
        1.0f, 0.0f,
        1.0f, 1.0f,
        0.0f, 1.0f,
    };
    private static final FloatBuffer TEXTURE_COORD_BUFFER = DirectBuffers.newDirectFloatBuffer_nativeOrder(TEXTURE_COORD_ARR);
    
    private static final int[] INDICES_ARR = new int[]{
        0, 1, 2,
        2, 3, 0
    };
    private static final IntBuffer INDICES_BUFFER = DirectBuffers.newDirectIntBuffer(INDICES_ARR);
    
    /**
     * Four (x,y) points.
     */
    private static final int VERTICES_POSITION_BUFFER_LENGTH = 4 * 2;
    private final FloatBuffer vertices_position_buffer = DirectBuffers.newDirectFloatBuffer_nativeOrder(VERTICES_POSITION_BUFFER_LENGTH);
    
    /*
     * When deciding whether we want to create a texture pixel array for
     * textures with only the columns that are in (dirty) clip,  we divide
     * the number of pixels in clip by the total number of pixels in
     * the client area, and we compare it to this value.
     * 
     * If a large enough texture array is already available, or if the ratio
     * is lower, we reuse or create the texture array to create a minimalistic
     * texture, else we just use the input array of pixels and create a texture
     * which spans the whole client area.
     * 
     * This allows to ensure fast painting of relatively small dirty regions,
     * without much GC in the long run, while ensuring that the corresponding
     * memory overhead stays small compared to the max size the client area
     * did reach.
     * 
     * We don't make these values configurable, for they might be hard to grasp
     * for users, and the fact that we don't use much additional memory should
     * make it mostly transparent to the user.
     */
    
    /**
     * Using 20 percents amount as threshold.
     */
    private static final double TEXTURE_ARRAY_RATIO_THRESHOLD = 0.2;
    
    /**
     * Using 20 percents amount as min growth factor.
     */
    private static final double TEXTURE_ARRAY_MIN_GROWTH_FACTOR = 1.2;
    
    private int[] tmpTexturePixelArr = LangUtils.EMPTY_INT_ARR;
    
    /*
     * 
     */
    
    private static final int POSITION_ATTRIBUTE_BEFORE = 0;
    private static final int TEXTURE_COORD_ATTRIBUTE_BEFORE = 1;
    
    private static final String ATTR_VSHADER_IN_POSITION = "vshader_in_position";
    private static final String ATTR_VSHADER_IN_TEXTURE_COORD = "vshader_in_texture_coord";
    
    /*
     * Shader program is created once and for all,
     * for it takes about 1ms to construct, which is negligible for
     * slow renderings, but not for dirty paintings.
     */
    
    private boolean shaderProgramCreated = false;
    private int shaderProgram;
    
    /**
     * Last GL used for rendering.
     * Using it as best effort instance to delete shader program on dispose
     * (seems to work, at least doesn't seem to complain).
     */
    private GL3ES3 lastGl;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public JoglPaintHelper() {
    }
    
    public void dispose() {
        if (this.shaderProgramCreated) {
            this.shaderProgramCreated = false;
            final GL3ES3 gl = this.lastGl;
            this.lastGl = null;
            if (gl != null) {
                gl.glDeleteProgram(this.shaderProgram);
            } else {
                // TODO jogl We are helpless.
                // Memory leak, or GL takes care of it on context destruction?
            }
        }
    }
    
    /**
     * @return The GLProfile our implementation is using.
     */
    public static GLProfile getGlProfiletoUse() {
        return GLProfile.get(GLProfile.GL3);
    }
    
    /**
     * NB: drawable can be a GLWindow.
     * 
     * @return The GL our implementation is using, or null if the drawable
     *         in not in a good shape and doesn't have one, which can happen
     *         if used after close.
     */
    public static GL3ES3 getGlToUse(GLAutoDrawable drawable) {
        final GL gl = drawable.getGL();
        if (gl == null) {
            // Can happen in case of usage after close.
            return null;
        }
        return gl.getGL3ES3();
    }
    
    /*
     * 
     */
    
    public static InterfaceColorTypeHelper getArrayColorHelper() {
        return PremulNativeRgbaHelper.getInstance();
    }

    public static int getArrayColor32FromArgb32(int argb32) {
        return BindingColorUtils.toPremulNativeRgba32FromArgb32(argb32);
    }
    
    public static int getArgb32FromArrayColor32(int premulColor32) {
        return BindingColorUtils.toArgb32FromPremulNativeRgba32(premulColor32);
    }
    
    /*
     * 
     */
    
    public static int toInvertedArrayColor32(int premulColor32) {
        return BindingColorUtils.toInvertedPremulNativeRgba32(premulColor32);
    }
    
    public static int getArrayColorAlpha8(int premulColor32) {
        return BindingColorUtils.getNativeRgba32Alpha8(premulColor32);
    }
    
    public static int blendArrayColor32(int srcPremulColor32, int dstPremulColor32) {
        return BindingColorUtils.blendPremulNativeRgba32(srcPremulColor32, dstPremulColor32);
    }
    
    /*
     * 
     */
    
    public void paintPixelsIntoOpenGl(
        ScaleHelper scaleHelper,
        GPoint clientSpansInOs,
        GPoint bufferPosInCliInOs,
        List<GRect> paintedRectList,
        //
        IntArrayGraphicBuffer bufferInBd,
        GLWindow window) {
        
        final GL3ES3 gl = JoglPaintHelper.getGlToUse(window);
        this.lastGl = gl;
        
        /*
         * 
         */
        
        final int[] bufferArr = bufferInBd.getPixelArr();
        final int bufferArrScanlineStride = bufferInBd.getScanlineStride();
        final int bufferWidth = bufferInBd.getWidth();
        final int bufferHeight = bufferInBd.getHeight();
        
        if (MUST_NOT_RENDER_IF_EMPTY_AREA
            && ((bufferWidth <= 0)
                || (bufferHeight <= 0))) {
            return;
        }
        
        /*
         * Disabling unused stuffs, in case it would
         * make things lighter.
         */
        
        gl.glDisable(GL.GL_DEPTH_TEST);
        gl.glDisable(GL.GL_BLEND);
        
        /*
         * GL arrays and buffers (out of loop).
         */
        
        final int vao = createAndBindVao(gl);
        
        // EAB is part of VAO.
        final int eab = createAndBindEab(gl);
        
        /*
         * 
         */
        
        for (GRect paintedRect : paintedRectList) {
            
            /*
             * Texture.
             */
            
            final MyTextureData textureData = computeTextureData(
                bufferArr,
                bufferArrScanlineStride,
                bufferWidth,
                bufferHeight,
                paintedRect);
            
            final int texture = createTexture(
                gl,
                textureData);
            
            this.setTextureVerticesPositions(
                scaleHelper,
                bufferPosInCliInOs.x(),
                bufferPosInCliInOs.y(),
                clientSpansInOs.x(),
                clientSpansInOs.y(),
                textureData.textureRect);
            
            /*
             * GL arrays and buffers (in loop).
             */
            
            // VBO is not part of VAO.
            final int vbo = createAndBindVbo(
                gl,
                this.vertices_position_buffer);
            
            /*
             * Shader program.
             */
            
            shaderProgramBindingBeforeCreation(gl);
            
            if (!this.shaderProgramCreated) {
                this.shaderProgram = createShadersAndShaderProgram(gl);
                this.shaderProgramCreated = true;
            } else {
                /*
                 * TODO jogl Seems it's enough to just call this
                 * after shader creation, but it shouldn't hurt
                 * to call it before each use.
                 */
                gl.glUseProgram(this.shaderProgram);
            }
            
            shaderProgramBindingAfterCreation(gl, this.shaderProgram);
            
            /*
             * Rendering.
             * 
             * Must not "clear" before, first because it's useless,
             * and second because it would not allow for dirty painting.
             */
            
            final int mode = GL.GL_TRIANGLES;
            final int count = 6;
            final int type = GL.GL_UNSIGNED_INT;
            final long indices2 = 0L;
            gl.glDrawElements(mode, count, type, indices2);
            
            /*
             * 
             */
            
            glDeleteTexture(gl, texture);
            glDeleteBuffer(gl, vbo);
        }
        glDeleteBuffer(gl, eab);
        glDeleteVertexArray(gl, vao);
    }
    
    public void flushPainting(GLWindow window) {
        // Can be null if flushing after close.
        final GL3ES3 gl = JoglPaintHelper.getGlToUse(window);
        if (gl != null) {
            gl.glFlush();
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /*
     * GL
     */
    
    private static int glGenVertexArray(GL3ES3 gl) {
        final int[] intArr = new int[1];
        gl.glGenVertexArrays(1, intArr, 0);
        return intArr[0];
    }
    
    private static int glGenBuffer(GL3ES3 gl) {
        final int[] intArr = new int[1];
        gl.glGenBuffers(1, intArr, 0);
        return intArr[0];
    }
    
    private static int glGenTexture(GL3ES3 gl) {
        final int[] intArr = new int[1];
        gl.glGenTextures(1, intArr, 0);
        return intArr[0];
    }
    
    private static void glDeleteTexture(GL3ES3 gl, int texture) {
        gl.glDeleteTextures(1, new int[]{texture}, 0);
    }
    
    private static void glDeleteBuffer(GL3ES3 gl, int buffer) {
        gl.glDeleteBuffers(1, new int[]{buffer}, 0);
    }
    
    private static void glDeleteVertexArray(GL3ES3 gl, int vao) {
        gl.glDeleteVertexArrays(1, new int[]{vao}, 0);
    }
    
    /*
     * Texture.
     */
    
    private MyTextureData computeTextureData(
        int[] bufferArr,
        int bufferArrScanlineStride,
        int bufferWidth,
        int bufferHeight,
        GRect clip) {
        
        final IntBuffer texturePixelsBuffer;
        final int texturePixelsScanlineStride;
        final GRect textureRect;
        
        /*
         * TODO jogl When we only want to paint a part of client area:
         * glTexImage2D allows to ignore first and last rows of the
         * specified pixel array, but not first and last columns,
         * so to do that we need to use another pixel array
         * (not to mess up the specified one) and copy pixels
         * in clip into it.
         * To avoid much garbage, we use a same internal array for it,
         * so only do it if the amount of pixels in clip fits in this array.
         */
        
        final boolean mustUseInternalArr;
        
        final boolean areAllColumnsInClip =
            (clip.xSpan() == bufferWidth);
        if (areAllColumnsInClip) {
            mustUseInternalArr = false;
        } else {
            final int pixelCountInClip = clip.area();
            
            int[] internalArr = this.tmpTexturePixelArr;
            if (internalArr.length >= pixelCountInClip) {
                mustUseInternalArr = true;
            } else {
                final int pixelCountInBuffer =
                    NbrsUtils.timesExact(
                        bufferWidth,
                        bufferHeight);
                final double ratio = pixelCountInClip / (double) pixelCountInBuffer;
                if (ratio <= TEXTURE_ARRAY_RATIO_THRESHOLD) {
                    // Creating large enough array.
                    // No overflow issue as long as threshold is small enough.
                    final int newTmpArrLength = Math.max(
                        pixelCountInClip,
                        (int) (TEXTURE_ARRAY_MIN_GROWTH_FACTOR * internalArr.length));
                    internalArr = new int[newTmpArrLength];
                    this.tmpTexturePixelArr = internalArr;
                    mustUseInternalArr = true;
                } else {
                    mustUseInternalArr = false;
                }
            }
        }
        
        if (mustUseInternalArr) {
            final int[] internalArr = this.tmpTexturePixelArr;
            /*
             * Copying pixels into internal array.
             */
            final int texXSpan = clip.xSpan();
            for (int y = clip.y(); y <= clip.yMax(); y++) {
                final int srcOffset = clip.x() + y * bufferArrScanlineStride;
                final int dstOffset = (y - clip.y()) * texXSpan;
                System.arraycopy(
                    bufferArr,
                    srcOffset,
                    internalArr,
                    dstOffset,
                    texXSpan);
            }
            /*
             * 
             */
            final int length = clip.area();
            texturePixelsBuffer = IntBuffer.wrap(internalArr, 0, length);
            texturePixelsScanlineStride = clip.xSpan();
            textureRect = clip;
        } else {
            final int offset = clip.y() * bufferArrScanlineStride;
            final int length = bufferArrScanlineStride * clip.ySpan();
            texturePixelsBuffer = IntBuffer.wrap(bufferArr, offset, length);
            texturePixelsScanlineStride = bufferArrScanlineStride;
            textureRect = GRect.valueOf(
                0,
                clip.y(),
                bufferWidth,
                clip.ySpan());
        }
        
        return new MyTextureData(
            texturePixelsBuffer,
            texturePixelsScanlineStride,
            textureRect);
    }
    
    private static int createTexture(
        GL3ES3 gl,
        MyTextureData textureData) {
        
        final IntBuffer texturePixelsBuffer = textureData.texturePixelsBuffer;
        final int texturePixelsScanlineStride = textureData.texturePixelsScanlineStride;
        final int textureWidth = textureData.textureRect.xSpan();
        final int textureHeight = textureData.textureRect.ySpan();
        
        gl.glPixelStorei(GL3ES3.GL_UNPACK_ROW_LENGTH, texturePixelsScanlineStride);
        try {
            final int texture = glGenTexture(gl);
            gl.glBindTexture(GL.GL_TEXTURE_2D, texture);
            
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
            
            gl.glTexImage2D(
                GL.GL_TEXTURE_2D,
                0,
                GL.GL_RGBA8,
                textureWidth,
                textureHeight,
                0,
                GL.GL_RGBA,
                GL.GL_UNSIGNED_BYTE,
                //
                texturePixelsBuffer);
            
            return texture;
        } finally {
            gl.glPixelStorei(GL3ES3.GL_UNPACK_ROW_LENGTH, 0);
        }
    }
    
    private void setTextureVerticesPositions(
        ScaleHelper scaleHelper,
        int clientXInBufferInOs,
        int clientYInBufferInOs,
        int clientXSpanInOs,
        int clientYSpanInOs,
        GRect textureRect) {
        
        /*
         * To convert texture coordinates, from buffer (frame 1)
         * into client (frame 2), when both use binding pixels.
         * x2 = x1 + bx
         * y2 = y1 + by
         */
        
        final float scaleInv = (float) scaleHelper.getScaleInv();
        final float clientXSpanInBd = clientXSpanInOs * scaleInv;
        final float clientYSpanInBd = clientYSpanInOs * scaleInv;
        final float bx = clientXInBufferInOs * scaleInv;
        final float by = clientYInBufferInOs * scaleInv;
        
        /*
         * 
         */
        
        // +1 for max coordinate to compute ratio properly,
        // as if integer coordinates were on pixels boundaries,
        // not pixels centers.
        final float texXMinRatio = (bx + textureRect.x()) / (float) clientXSpanInBd;
        final float texXMaxRatio = (bx + textureRect.xMax() + 1.0f) / (float) clientXSpanInBd;
        final float texYMinRatio = (by + textureRect.y()) / (float) clientYSpanInBd;
        final float texYMaxRatio = (by + textureRect.yMax() + 1.0f) / (float) clientYSpanInBd;
        //
        float vp_xrMin = ((2.0f * texXMinRatio) - 1.0f);
        float vp_yrMin = ((2.0f * texYMinRatio) - 1.0f);
        float vp_xrMax = ((2.0f * texXMaxRatio) - 1.0f);
        float vp_yrMax = ((2.0f * texYMaxRatio) - 1.0f);
        
        final FloatBuffer vertices_position_buffer = this.vertices_position_buffer;
        
        // Clear should be useless but doesn't hurt.
        vertices_position_buffer.clear();
        
        vertices_position_buffer.put(vp_xrMin);
        vertices_position_buffer.put(vp_yrMin);
        //
        vertices_position_buffer.put(vp_xrMax);
        vertices_position_buffer.put(vp_yrMin);
        //
        vertices_position_buffer.put(vp_xrMax);
        vertices_position_buffer.put(vp_yrMax);
        //
        vertices_position_buffer.put(vp_xrMin);
        vertices_position_buffer.put(vp_yrMax);
        
        vertices_position_buffer.flip();
    }
    
    /*
     * GL arrays and buffers.
     */
    
    /**
     * @return The vao.
     */
    private static int createAndBindVao(GL3ES3 gl) {
        final int vao = glGenVertexArray(gl);
        gl.glBindVertexArray(vao);
        return vao;
    }
    
    /**
     * @return The vbo.
     */
    private static int createAndBindVbo(
        GL3ES3 gl,
        FloatBuffer vertices_position_buffer) {
        /*
         * Actually just generates a "free integer".
         */
        final int vbo = glGenBuffer(gl);
        /*
         * Makes our VBO the current GL_ARRAY_BUFFER to work with.
         * 
         * "A buffer object binding created with glBindBuffer remains active
         * until a different buffer object name is bound to the same target,
         * or until the bound buffer object is deleted with glDeleteBuffers."
         */
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo);
        
        gl.glBufferData(
            GL.GL_ARRAY_BUFFER,
            VERTICES_POSITION_BUFFER_LENGTH * (long) Float_BYTES
            + TEXTURE_COORD_ARR.length * (long) Float_BYTES,
            (Buffer) null,
            GL.GL_STATIC_DRAW);
        {
            final long offset = 0L;
            final long length = VERTICES_POSITION_BUFFER_LENGTH * (long) Float_BYTES;
            gl.glBufferSubData(GL.GL_ARRAY_BUFFER, offset, length, vertices_position_buffer);
        }
        {
            final long offset = VERTICES_POSITION_BUFFER_LENGTH * (long) Float_BYTES;
            final long length = TEXTURE_COORD_ARR.length * (long) Float_BYTES;
            gl.glBufferSubData(GL.GL_ARRAY_BUFFER, offset, length, TEXTURE_COORD_BUFFER);
        }
        
        return vbo;
    }
    
    /**
     * @return The eab.
     */
    private static int createAndBindEab(GL3ES3 gl) {
        final int eab = glGenBuffer(gl);
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, eab);
        
        gl.glBufferData(
            GL.GL_ELEMENT_ARRAY_BUFFER,
            INDICES_ARR.length * (long) Integer_BYTES,
            INDICES_BUFFER,
            GL.GL_STATIC_DRAW);
        
        return eab;
    }
    
    /*
     * Shader program.
     */
    
    /**
     * @return A string array containing shader code as a single string,
     *         with newline added after each specified line.
     */
    private static String[] toSingleStringWithNewlines(String... lines) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            sb.append(lines[i]).append("\n");
        }
        return new String[]{sb.toString()};
    }
    
    private static int createVertexShader(GL3ES3 gl, String... lines) {
        final int vertexShader = gl.glCreateShader(GL3ES3.GL_VERTEX_SHADER);
        final String[] flines = toSingleStringWithNewlines(lines);
        final int[] flengths = new int[]{flines[0].length()};
        gl.glShaderSource(vertexShader, 1, flines, flengths, 0);
        gl.glCompileShader(vertexShader);
        
        final int[] statusArr = new int[1];
        gl.glGetShaderiv(vertexShader, GL3ES3.GL_COMPILE_STATUS, statusArr, 0);
        final int status = statusArr[0];
        if (status != GL.GL_TRUE) {
            int[] logLength = new int[1];
            gl.glGetShaderiv(vertexShader, GL3ES3.GL_INFO_LOG_LENGTH, logLength, 0);
            byte[] log = new byte[logLength[0]];
            gl.glGetShaderInfoLog(vertexShader, logLength[0], (int[]) null, 0, log, 0);
            throw new BindingError(new String(log));
        }
        return vertexShader;
    }
    
    private static int createFragmentShader(GL3ES3 gl, String... lines) {
        final int fragmentShader = gl.glCreateShader(GL3ES3.GL_FRAGMENT_SHADER);
        final String[] flines = toSingleStringWithNewlines(lines);
        final int[] flengths = new int[]{flines[0].length()};
        gl.glShaderSource(fragmentShader, 1, flines, flengths, 0);
        gl.glCompileShader(fragmentShader);
        
        final int[] statusArr = new int[1];
        gl.glGetShaderiv(fragmentShader, GL3ES3.GL_COMPILE_STATUS, statusArr, 0);
        final int status = statusArr[0];
        if (status != GL.GL_TRUE) {
            int[] logLength = new int[1];
            gl.glGetShaderiv(fragmentShader, GL3ES3.GL_INFO_LOG_LENGTH, logLength, 0);
            byte[] log = new byte[logLength[0]];
            gl.glGetShaderInfoLog(fragmentShader, logLength[0], (int[]) null, 0, log, 0);
            throw new BindingError(new String(log));
        }
        
        return fragmentShader;
    }
    
    /**
     * Deletes the specified shaders once the program has been created.
     * Calls glUseProgram(...).
     */
    private static int createShaderProgram(
        GL3ES3 gl,
        int vertexShader,
        int fragmentShader,
        String fragmentShaderOutAttr) {
        final int shaderProgram = gl.glCreateProgram();
        
        gl.glAttachShader(shaderProgram, vertexShader);
        gl.glAttachShader(shaderProgram, fragmentShader);
        
        // Now can delete them.
        gl.glDeleteShader(vertexShader);
        gl.glDeleteShader(fragmentShader);
        
        if (false) {
            /*
             * TODO jogl The clean way would be to use glBindFragDataLocation,
             * but it's not in GL3ES3.
             * Fortunately, for some possibly obvious reasons,
             * we can just not do it, and keep GL ES compatibility.
             */
            ((GL3) gl).glBindFragDataLocation(shaderProgram, 0, fragmentShaderOutAttr);
        }
        
        gl.glLinkProgram(shaderProgram);
        
        final int[] statusArr = new int[1];
        gl.glGetProgramiv(shaderProgram, GL3ES3.GL_LINK_STATUS, statusArr, 0);
        final int status = statusArr[0];
        if (status != GL.GL_TRUE) {
            // TODO Factor all these.
            int[] logLength = new int[1];
            gl.glGetProgramiv(shaderProgram, GL3ES3.GL_INFO_LOG_LENGTH, logLength, 0);
            byte[] log = new byte[logLength[0]];
            gl.glGetProgramInfoLog(shaderProgram, logLength[0], (int[]) null, 0, log, 0);
            throw new BindingError(new String(log));
        }
        
        gl.glUseProgram(shaderProgram);
        
        return shaderProgram;
    }
    
    private static void shaderProgramBindingBeforeCreation(GL3ES3 gl) {
        if (MUST_BIND_BEFORE_PROG_CREATION) {
            {
                gl.glEnableVertexAttribArray(POSITION_ATTRIBUTE_BEFORE);
                
                final int count = 2;
                final int type = GL.GL_FLOAT;
                final boolean normalized = false;
                final int stride = 2 * Float_BYTES;
                final long pointer = 0L * Float_BYTES;
                gl.glVertexAttribPointer(POSITION_ATTRIBUTE_BEFORE, count, type, normalized, stride, pointer);
            }
            
            {
                gl.glEnableVertexAttribArray(TEXTURE_COORD_ATTRIBUTE_BEFORE);
                
                final int count = 2;
                final int type = GL.GL_FLOAT;
                final boolean normalized = false;
                final int stride = 2 * Float_BYTES;
                final long pointer = VERTICES_POSITION_BUFFER_LENGTH * Float_BYTES;
                gl.glVertexAttribPointer(TEXTURE_COORD_ATTRIBUTE_BEFORE, count, type, normalized, stride, pointer);
            }
        }
    }
    
    private static void shaderProgramBindingAfterCreation(
        GL3ES3 gl,
        int shaderProgram) {
        if (MUST_BIND_BEFORE_PROG_CREATION) {
            gl.glBindAttribLocation(shaderProgram, POSITION_ATTRIBUTE_BEFORE, ATTR_VSHADER_IN_POSITION);
        } else {
            final int position_attribute = gl.glGetAttribLocation(shaderProgram, ATTR_VSHADER_IN_POSITION);
            
            // Enable the attribute
            gl.glEnableVertexAttribArray(position_attribute);
            
            // Specify how the data for position can be accessed
            {
                final int count = 2;
                final int type = GL.GL_FLOAT;
                final boolean normalized = false;
                final int stride = 2 * Float_BYTES;
                final long pointer = 0L;
                gl.glVertexAttribPointer(position_attribute, count, type, normalized, stride, pointer);
            }
        }
        
        if (MUST_BIND_BEFORE_PROG_CREATION) {
            gl.glBindAttribLocation(shaderProgram, TEXTURE_COORD_ATTRIBUTE_BEFORE, ATTR_VSHADER_IN_TEXTURE_COORD);
        } else {
            // Get the location of the attributes that enters in the vertex shader
            final int texture_coord_attribute = gl.glGetAttribLocation(shaderProgram, ATTR_VSHADER_IN_TEXTURE_COORD);
            
            // Enable the attribute
            gl.glEnableVertexAttribArray(texture_coord_attribute);
            
            // Specify how the data for position can be accessed
            {
                final int count = 2;
                final int type = GL.GL_FLOAT;
                final boolean normalized = false;
                // TODO jogl 0 seems to work here (???).
                final int stride = 2 * Float_BYTES;
                final long pointer = VERTICES_POSITION_BUFFER_LENGTH * (long) Float_BYTES;
                gl.glVertexAttribPointer(texture_coord_attribute, count, type, normalized, stride, pointer);
            }
        }
    }
    
    private static int createShadersAndShaderProgram(GL3ES3 gl) {
        // True because our int[] contains rows from top to bottom,
        // and OpenGL expects them from bottom to top.
        final boolean mustFlipTexture = true;
        
        final String vsInPosAttr = ATTR_VSHADER_IN_POSITION;
        final String vsInTexCoordAttr = ATTR_VSHADER_IN_TEXTURE_COORD;
        
        final String vsOutTextureCoordAttr = "vshader_out_texture_coord";
        
        final String fsOutAttr = "fshader_out_color";
        
        /*
         * 
         */
        
        final int vertexShader = createVertexShader(
            gl,
            "#version 150",
            "",
            "in " + (mustFlipTexture ? "vec2" : "vec4") + " " + vsInPosAttr + ";",
            "in vec2 " + vsInTexCoordAttr + ";",
            "",
            "out vec2 " + vsOutTextureCoordAttr + ";",
            "",
            "void main() {",
            "    gl_Position = " + (mustFlipTexture ? "vec4(" + vsInPosAttr + ".s, -" + vsInPosAttr + ".t, 0.0f, 1.0f)" : vsInPosAttr) + ";",
            "    " + vsOutTextureCoordAttr + " = " + vsInTexCoordAttr + ";",
            "}");
        
        final int fragmentShader = createFragmentShader(
            gl,
            "#version 150",
            "",
            "uniform sampler2D texture_sampler;",
            "",
            "in vec2 " + vsOutTextureCoordAttr + ";",
            "",
            // Output is gl_FragColor if not specified.
            "out vec4 " + fsOutAttr + ";",
            "",
            "void main() {",
            "    vec4 premulRgba = texture(texture_sampler, " + vsOutTextureCoordAttr + ");",
            // De-alpha-premultiplying color components.
            "    " + fsOutAttr + " = vec4(premulRgba.rgb/premulRgba.a, premulRgba.a);",
            "}");
        
        return createShaderProgram(
            gl,
            vertexShader,
            fragmentShader,
            fsOutAttr);
    }
    
    /*
     * 
     */
    
    /**
     * Causes implicit synchronization between CPU and GPU,
     * so you should use it only for debug purposes.
     */
    private static void checkNoGlError(GL3ES3 gl) {
        final int flag = gl.glGetError();
        if (flag != GL.GL_NO_ERROR) {
            if (flag == GL.GL_INVALID_ENUM) {
                throw new BindingError("GL_INVALID_ENUM");
            }
            if (flag == GL.GL_INVALID_VALUE) {
                throw new BindingError("GL_INVALID_VALUE");
            }
            if (flag == GL.GL_INVALID_OPERATION) {
                throw new BindingError("GL_INVALID_OPERATION");
            }
            if (flag == GL.GL_INVALID_FRAMEBUFFER_OPERATION) {
                throw new BindingError("GL_INVALID_FRAMEBUFFER_OPERATION");
            }
            if (flag == GL.GL_OUT_OF_MEMORY) {
                throw new BindingError("GL_OUT_OF_MEMORY");
            }
            if (flag == GL3ES3.GL_STACK_UNDERFLOW) {
                throw new BindingError("GL_STACK_UNDERFLOW");
            }
            if (flag == GL3ES3.GL_STACK_OVERFLOW) {
                throw new BindingError("GL_STACK_OVERFLOW");
            }
            throw new BindingError("unknown error flag: " + flag);
        }
    }
}
