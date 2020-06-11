/*
 * Copyright 2019-2020 Jeff Hain
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
package net.jolikit.bwd.impl.lwjgl3;

import java.nio.IntBuffer;
import java.util.List;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLCapabilities;

import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.utils.basics.BindingError;
import net.jolikit.bwd.impl.utils.basics.PixelCoordsConverter;
import net.jolikit.bwd.impl.utils.graphics.BindingColorUtils;
import net.jolikit.bwd.impl.utils.graphics.DirectBuffers;
import net.jolikit.bwd.impl.utils.graphics.IntArrayGraphicBuffer;

public class LwjglPaintHelper {

    /*
     * TODO lwjgl If using glTexImage2D with an IntBuffer,
     * it needs to be direct else it just silently doesn't work,
     * so we can't wrap an IntBuffer around our int array of pixels
     * to ensure some offset to ignore rows that are out of clip
     * (as we can do with jogl).
     * As a result, for dirty paintings, we use a thread-local
     * direct buffer, and if it's not large enough, we just use
     * a texture covering the whole client area.
     */

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    /**
     * Either seem to work.
     * If true, using glBindAttribLocation(...),
     * else, using glGetAttribLocation(...).
     */
    private static final boolean MUST_BIND_BEFORE_PROG_CREATION = false;
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private static class MyTextureData {
        /**
         * int[] or IntBuffer.
         */
        final Object texturePixels;
        final int texturePixelsScanlineStride;
        /**
         * Rectangle in client area, from which GL vertices coordinates
         * are computed.
         */
        final GRect textureRect;
        public MyTextureData(
                Object texturePixels,
                int texturePixelsScanlineStride,
                GRect textureRect) {
            this.texturePixels = texturePixels;
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
     * 
     */

    private static final float[] TEXTURE_COORD_ARR = new float[]{
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f,
    };

    private static final int[] INDICES_ARR = new int[]{
            0, 1, 2,
            2, 3, 0
    };

    /**
     * Four (x,y) points.
     */
    private static final int VERTICES_POSITION_ARR_LENGTH = 4 * 2;
    private final float[] vertices_position_arr = new float[VERTICES_POSITION_ARR_LENGTH];
    
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
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public LwjglPaintHelper() {
    }
    
    public void dispose() {
        if (this.shaderProgramCreated) {
            this.shaderProgramCreated = false;
            GL20.glDeleteProgram(this.shaderProgram);
        }
    }
    
    public static int getArrayColor32FromArgb32(int argb32) {
        return BindingColorUtils.toPremulNativeRgba32FromArgb32(argb32);
    }
    
    public static int getArgb32FromArrayColor32(int premulColor32) {
        return BindingColorUtils.toArgb32FromPremulNativeRgba32(premulColor32);
    }
    
    public static int getArgb32FromNonPremulArrayColor32(int color32) {
        return BindingColorUtils.toArgb32FromNativeRgba32(color32);
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
            PixelCoordsConverter pixelCoordsConverter,
            IntArrayGraphicBuffer offscreenBuffer,
            List<GRect> clipList,
            long window,
            GLCapabilities capabilities) {
        
        final int[] clientPixelArr = offscreenBuffer.getPixelArr();
        final int clientPixelArrScanlineStride = offscreenBuffer.getScanlineStride();

        final int clientWidth = offscreenBuffer.getWidth();
        final int clientHeight = offscreenBuffer.getHeight();

        // Make the OpenGL context current
        GLFW.glfwMakeContextCurrent(window);

        GL.setCapabilities(capabilities);

        final int clientWidthInDevice = pixelCoordsConverter.computeXSpanInDevicePixel(clientWidth);
        final int clientHeightInDevice = pixelCoordsConverter.computeYSpanInDevicePixel(clientHeight);
        
        /*
         * When a context is first attached to a window, the viewport will be set to the dimensions of the window,
         * i.e. the equivalent of glViewport(0,0,width,height). So it's only necessary to call glViewport()
         * when the window size changes or if you want to set the viewport to something other than the entire window.
         * 
         * Need to call that else the (-1,1) coordinate ranges of OpenGL
         * still correspond to initial width and height.
         */
        GL11.glViewport(0, 0, clientWidthInDevice, clientHeightInDevice);

        /*
         * Disabling unused stuffs, in case it would
         * make things lighter.
         */
        
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        
        /*
         * GL arrays and buffers (out of loop).
         */

        final int vao = createAndBindVao();
        
        // EAB is part of VAO.
        final int eab = createAndBindEab();
        
        /*
         * 
         */

        for (GRect clip : clipList) {

            /*
             * Texture.
             */
            
            final MyTextureData textureData = computeTextureData(
                    clientPixelArr,
                    clientPixelArrScanlineStride,
                    clientWidth,
                    clientHeight,
                    clip);
            
            final int texture = createTexture(textureData);
            
            this.setTextureVerticesPositions(
                    clientWidth,
                    clientHeight,
                    textureData.textureRect);

            /*
             * GL arrays and buffers (in loop).
             */
            
            // VBO is not part of VAO.
            final int vbo = createAndBindVbo(this.vertices_position_arr);
            
            /*
             * Shader program.
             */
            
            shaderProgramBindingBeforeCreation();
            
            if (!this.shaderProgramCreated) {
                this.shaderProgram = createShadersAndShaderProgram();
                this.shaderProgramCreated = true;
            } else {
                /*
                 * TODO lwjgl Seems it's enough to just call this
                 * after shader creation, but it shouldn't hurt
                 * to call it before each use.
                 */
                GL20.glUseProgram(this.shaderProgram);
            }
            
            shaderProgramBindingAfterCreation(this.shaderProgram);

            /*
             * Rendering.
             * 
             * Must not "clear" before, first because it's useless,
             * and second because it would not allow for dirty painting.
             */

            /*
             * GL_POINTS, GL_LINE_STRIP, GL_LINE_LOOP, GL_LINES, GL_LINE_STRIP_ADJACENCY, GL_LINES_ADJACENCY,
             * GL_TRIANGLE_STRIP, GL_TRIANGLE_FAN, GL_TRIANGLES, GL_TRIANGLE_STRIP_ADJACENCY, GL_TRIANGLES_ADJACENCY
             * and GL_PATCHES
             */
            final int mode = GL11.GL_TRIANGLES;
            final int count = 6;
            final int type = GL11.GL_UNSIGNED_INT;
            final long indices2 = 0L;
            GL11.glDrawElements(mode, count, type, indices2);

            /*
             * 
             */
            
            GL11.glDeleteTextures(texture);
            GL15.glDeleteBuffers(vbo);
        }
        
        /*
         * 
         */
        
        GL15.glDeleteBuffers(eab);
        GL30.glDeleteVertexArrays(vao);
    }
    
    public void flushPainting(
            long window,
            boolean glDoubleBuffered) {
        if (glDoubleBuffered) {
            GLFW.glfwSwapBuffers(window);
        } else {
            GL11.glFlush();
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /*
     * Texture.
     */
    
    /**
     * Computing texture pixels (in int[] or IntBuffer),
     * and texture bounds.
     * 
     * @param clip Clip in client coordinates.
     */
    private static MyTextureData computeTextureData(
            int[] clientPixelArr,
            int clientPixelArrScanlineStride,
            int clientWidth,
            int clientHeight,
            GRect clip) {
        
        final Object texturePixels;
        final int texturePixelsScanlineStride;
        final GRect textureRect;

        final IntBuffer directIbToUse;

        final boolean clipIsFull =
                (clip.xSpan() == clientWidth)
                && (clip.ySpan() == clientHeight);
        if (clipIsFull) {
            directIbToUse = null;
        } else {
            final int pixelCountInClip = clip.xSpan() * clip.ySpan();
            final IntBuffer directIB = DirectBuffers.getThreadLocalDirectIntBuffer_1024_sq_nativeOrder();
            if (directIB.capacity() >= pixelCountInClip) {
                directIbToUse = directIB;
            } else {
                directIbToUse = null;
            }
        }

        if (directIbToUse != null) {
            final IntBuffer directIb = directIbToUse;
            /*
             * Copying pixels into texture buffer.
             */
             final int texXSpan = clip.xSpan();
             directIb.clear();
             for (int y = clip.y(); y <= clip.yMax(); y++) {
                 final int srcOffset = clip.x() + y * clientPixelArrScanlineStride;
                 directIb.put(clientPixelArr, srcOffset, texXSpan);
             }
             directIb.flip();
             /*
              * 
              */
             texturePixels = directIb;
             texturePixelsScanlineStride = clip.xSpan();
             textureRect = clip;
        } else {
            texturePixels = clientPixelArr;
            texturePixelsScanlineStride = clientPixelArrScanlineStride;
            textureRect = GRect.valueOf(
                    0,
                    0,
                    clientWidth,
                    clientHeight);
        }
        
        return new MyTextureData(
                texturePixels,
                texturePixelsScanlineStride,
                textureRect);
    }
    
    private static int createTexture(MyTextureData textureData) {
        
        final Object texturePixels = textureData.texturePixels;
        final int texturePixelsScanlineStride = textureData.texturePixelsScanlineStride;
        final int textureWidth = textureData.textureRect.xSpan();
        final int textureHeight = textureData.textureRect.ySpan();
        
        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, texturePixelsScanlineStride);
        try {
            final int texture = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);

            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL13.GL_CLAMP_TO_BORDER);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL13.GL_CLAMP_TO_BORDER);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            
            if (texturePixels instanceof int[]) {
                GL11.glTexImage2D(
                        GL11.GL_TEXTURE_2D,
                        0,
                        GL11.GL_RGBA8,
                        textureWidth,
                        textureHeight,
                        0,
                        GL11.GL_RGBA,
                        GL11.GL_UNSIGNED_BYTE,
                        //
                        (int[]) texturePixels);
            } else {
                GL11.glTexImage2D(
                        GL11.GL_TEXTURE_2D,
                        0,
                        GL11.GL_RGBA8,
                        textureWidth,
                        textureHeight,
                        0,
                        GL11.GL_RGBA,
                        GL11.GL_UNSIGNED_BYTE,
                        //
                        (IntBuffer) texturePixels);
            }

            return texture;
        } finally {
            GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
        }
    }
    
    private void setTextureVerticesPositions(
            int clientWidth,
            int clientHeight,
            GRect textureRect) {

        // +1 for max coordinate to compute ratio properly,
        // as if integer coordinates were on pixels boundaries,
        // not pixels centers.
        final float texXMinRatio = textureRect.x() / (float) clientWidth;
        final float texXMaxRatio = (textureRect.xMax() + 1.0f) / (float) clientWidth;
        final float texYMinRatio = textureRect.y() / (float) clientHeight;
        final float texYMaxRatio = (textureRect.yMax() + 1.0f) / (float) clientHeight;
        //
        float vp_xrMin = ((2.0f * texXMinRatio) - 1.0f);
        float vp_yrMin = ((2.0f * texYMinRatio) - 1.0f);
        float vp_xrMax = ((2.0f * texXMaxRatio) - 1.0f);
        float vp_yrMax = ((2.0f * texYMaxRatio) - 1.0f);
        
        final float[] vertices_position_arr = this.vertices_position_arr;
        vertices_position_arr[0] = vp_xrMin;
        vertices_position_arr[1] = vp_yrMin;
        //
        vertices_position_arr[2] = vp_xrMax;
        vertices_position_arr[3] = vp_yrMin;
        //
        vertices_position_arr[4] = vp_xrMax;
        vertices_position_arr[5] = vp_yrMax;
        //
        vertices_position_arr[6] = vp_xrMin;
        vertices_position_arr[7] = vp_yrMax;
    }
    
    /*
     * GL arrays and buffers.
     */

    /**
     * @return The vbo.
     */
    private static int createAndBindVbo(float[] vertices_position_arr) {
        /*
         * Actually just generates a "free integer".
         */
        final int vbo = GL15.glGenBuffers();
        /*
         * Makes our VBO the current GL_ARRAY_BUFFER to work with.
         * 
         * "A buffer object binding created with glBindBuffer remains active
         * until a different buffer object name is bound to the same target,
         * or until the bound buffer object is deleted with glDeleteBuffers."
         */
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

        GL15.glBufferData(
                GL15.GL_ARRAY_BUFFER,
                vertices_position_arr.length * (long) Float_BYTES
                + TEXTURE_COORD_ARR.length * (long) Float_BYTES,
                GL15.GL_STATIC_DRAW);
        {
            final long offset = 0L;
            GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, offset, vertices_position_arr);
        }
        {
            final long offset = vertices_position_arr.length * (long) Float_BYTES;
            GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, offset, TEXTURE_COORD_ARR);
        }
        
        return vbo;
    }
    
    /**
     * @return The vao.
     */
    private static int createAndBindVao() {
        final int vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);
        return vao;
    }
    
    /**
     * @return The eab.
     */
    private static int createAndBindEab() {
        final int eab = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, eab);

        // Transfer the data from indices to eab.
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, INDICES_ARR, GL15.GL_STATIC_DRAW);
        
        return eab;
    }

    /*
     * Shader program.
     */
    
    private static String toStringShader(String... lines) {
        final StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line);
            sb.append('\n');
        }
        return sb.toString();
    }
    
    private static int createVertexShader(String... lines) {
        final String vertexSource = toStringShader(lines);

        final int vertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vertexShader, vertexSource);
        GL20.glCompileShader(vertexShader);

        final int status = GL20.glGetShaderi(vertexShader, GL20.GL_COMPILE_STATUS);
        if (status != GL11.GL_TRUE) {
            throw new BindingError(GL20.glGetShaderInfoLog(vertexShader));
        }
        return vertexShader;
    }
    
    private static int createFragmentShader(String... lines) {
        final String fragmentSource = toStringShader(lines);

        final int fragmentShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fragmentShader, fragmentSource);
        GL20.glCompileShader(fragmentShader);

        final int status = GL20.glGetShaderi(fragmentShader, GL20.GL_COMPILE_STATUS);
        if (status != GL11.GL_TRUE) {
            throw new BindingError(GL20.glGetShaderInfoLog(fragmentShader));
        }

        return fragmentShader;
    }

    /**
     * Deletes the specified shaders once the program has been created.
     * Calls glUseProgram(...).
     */
    private static int createShaderProgram(
            int vertexShader,
            int fragmentShader,
            String fragmentShaderOutAttr) {
        final int shaderProgram = GL20.glCreateProgram();
        
        GL20.glAttachShader(shaderProgram, vertexShader);
        GL20.glAttachShader(shaderProgram, fragmentShader);
        
        // Now can delete them.
        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);
        
        if (false) {
            /*
             * TODO lwjgl The clean way would be to use glBindFragDataLocation,
             * but it's not in GL ES (cf. jogl binding, which makes this
             * obvious).
             * Fortunately, for some possibly obvious reasons,
             * we can just not do it, and keep GL ES compatibility.
             */
            GL30.glBindFragDataLocation(shaderProgram, 0, fragmentShaderOutAttr);
        }
        
        GL20.glLinkProgram(shaderProgram);

        int status = GL20.glGetProgrami(shaderProgram, GL20.GL_LINK_STATUS);
        if (status != GL11.GL_TRUE) {
            throw new BindingError(GL20.glGetProgramInfoLog(shaderProgram));
        }

        GL20.glUseProgram(shaderProgram);
        
        return shaderProgram;
    }

    private static void shaderProgramBindingBeforeCreation() {
        if (MUST_BIND_BEFORE_PROG_CREATION) {
            {
                GL20.glEnableVertexAttribArray(POSITION_ATTRIBUTE_BEFORE);
                
                final int count = 2;
                final int type = GL11.GL_FLOAT;
                final boolean normalized = false;
                final int stride = 2 * Float_BYTES;
                final long pointer = 0L * Float_BYTES;
                GL20.glVertexAttribPointer(POSITION_ATTRIBUTE_BEFORE, count, type, normalized, stride, pointer);
            }
            
            {
                GL20.glEnableVertexAttribArray(TEXTURE_COORD_ATTRIBUTE_BEFORE);
                
                final int count = 2;
                final int type = GL11.GL_FLOAT;
                final boolean normalized = false;
                final int stride = 2 * Float_BYTES;
                final long pointer = VERTICES_POSITION_ARR_LENGTH * Float_BYTES;
                GL20.glVertexAttribPointer(TEXTURE_COORD_ATTRIBUTE_BEFORE, count, type, normalized, stride, pointer);
            }
        }
    }
    
    private static void shaderProgramBindingAfterCreation(int shaderProgram) {
        if (MUST_BIND_BEFORE_PROG_CREATION) {
            GL20.glBindAttribLocation(shaderProgram, POSITION_ATTRIBUTE_BEFORE, ATTR_VSHADER_IN_POSITION);
        } else {
            final int position_attribute = GL20.glGetAttribLocation(shaderProgram, ATTR_VSHADER_IN_POSITION);

            // Enable the attribute
            GL20.glEnableVertexAttribArray(position_attribute);

            // Specify how the data for position can be accessed
            {
                final int count = 2;
                final int type = GL11.GL_FLOAT;
                final boolean normalized = false;
                // NB: 0 seems to work here (???).
                final int stride = 2 * Float_BYTES;
                final long pointer = 0L;
                GL20.glVertexAttribPointer(position_attribute, count, type, normalized, stride, pointer);
            }
        }

        if (MUST_BIND_BEFORE_PROG_CREATION) {
            GL20.glBindAttribLocation(shaderProgram, TEXTURE_COORD_ATTRIBUTE_BEFORE, ATTR_VSHADER_IN_TEXTURE_COORD);
        } else {
            // Get the location of the attributes that enters in the vertex shader
            final int texture_coord_attribute = GL20.glGetAttribLocation(shaderProgram, ATTR_VSHADER_IN_TEXTURE_COORD);

            // Enable the attribute
            GL20.glEnableVertexAttribArray(texture_coord_attribute);

            // Specify how the data for position can be accessed
            {
                final int count = 2;
                final int type = GL11.GL_FLOAT;
                final boolean normalized = false;
                // TODO lwjgl 0 seems to work here (???).
                final int stride = 2 * Float_BYTES;
                final long pointer = VERTICES_POSITION_ARR_LENGTH * (long) Float_BYTES;
                GL20.glVertexAttribPointer(texture_coord_attribute, count, type, normalized, stride, pointer);
            }
        }
    }
    
    /**
     * @return The shader program.
     */
    private static int createShadersAndShaderProgram() {
        
        // True because our int[] contains rows from top to bottom,
        // and OpenGL expects them from bottom to top.
        final boolean mustFlipTexture = true;

        final String vsInPosAttr = ATTR_VSHADER_IN_POSITION;
        final String vsInTexCoordAttr = ATTR_VSHADER_IN_TEXTURE_COORD;
        
        final String vsOutTextureCoordAttr = "vshader_out_texture_coord";
        
        final String fsOutAttr = "fshader_out_color";
        
        final int vertexShader = createVertexShader(
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
                "#version 150",
                "",
                "uniform sampler2D texture_sampler;",
                "",
                "in vec2 " + vsOutTextureCoordAttr + ";",
                "",
                "out vec4 " + fsOutAttr + ";",
                "",
                "void main() {",
                "    vec4 premulRgba = texture(texture_sampler, " + vsOutTextureCoordAttr + ");",
                // De-alpha-premultiplying color components.
                "    " + fsOutAttr + " = vec4(premulRgba.rgb/premulRgba.a, premulRgba.a);",
                "}");

        return createShaderProgram(
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
    private static void checkNoGlError() {
        final int flag = GL11.glGetError();
        if (flag != GL11.GL_NO_ERROR) {
            if (flag == GL11.GL_INVALID_ENUM) {
                throw new BindingError("GL_INVALID_ENUM");
            }
            if (flag == GL11.GL_INVALID_VALUE) {
                throw new BindingError("GL_INVALID_VALUE");
            }
            if (flag == GL11.GL_INVALID_OPERATION) {
                throw new BindingError("GL_INVALID_OPERATION");
            }
            if (flag == GL30.GL_INVALID_FRAMEBUFFER_OPERATION) {
                throw new BindingError("GL_INVALID_FRAMEBUFFER_OPERATION");
            }
            if (flag == GL11.GL_OUT_OF_MEMORY) {
                throw new BindingError("GL_OUT_OF_MEMORY");
            }
            if (flag == GL11.GL_STACK_UNDERFLOW) {
                throw new BindingError("GL_STACK_UNDERFLOW");
            }
            if (flag == GL11.GL_STACK_OVERFLOW) {
                throw new BindingError("GL_STACK_OVERFLOW");
            }
            throw new BindingError("unknown error flag: " + flag);
        }
    }
}
