/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.jolikit.bwd.impl.utils.fontbox;

import java.io.IOException;

/**
 * A table in a true type font.
 * 
 * @author Ben Litchfield
 */
public class GlyphTable extends TTFTable
{
    /**
     * Tag to identify this table.
     */
    public static final String TAG = "glyf";

    private GlyphData[] glyphs;

    // lazy table reading
    private TTFDataStream data;
    private IndexToLocationTable loca;
    private int numGlyphs;
    
    private int cached = 0;
    
    /**
     * Don't even bother to cache huge fonts.
     */
    private static final int MAX_CACHE_SIZE = 5000;
    
    /**
     * Don't cache more glyphs than this.
     */
    private static final int MAX_CACHED_GLYPHS = 100;

    GlyphTable(TrueTypeFont font)
    {
        super(font);
    }

    /**
     * This will read the required data from the stream.
     * 
     * @param ttf The font that is being read.
     * @param data The stream to read the data from.
     * @throws IOException If there is an error reading the data.
     */
    @Override
    public void read(TrueTypeFont ttf, TTFDataStream data) throws IOException
    {
        loca = ttf.getIndexToLocation();
        numGlyphs = ttf.getNumberOfGlyphs();

        if (numGlyphs < MAX_CACHE_SIZE)
        {
            // don't cache the huge fonts to save memory
            glyphs = new GlyphData[numGlyphs];
        }

        // we don't actually read the complete table here because it can contain tens of thousands of glyphs
        this.data = data;
        initialized = true;
    }

    /**
     * Returns all glyphs. This method can be very slow.
     */
    public GlyphData[] getGlyphs() throws IOException
    {
        synchronized (font)
        {
            // the glyph offsets
            long[] offsets = loca.getOffsets();

            // the end of the glyph table
            // should not be 0, but sometimes is, see PDFBOX-2044
            // structure of this table: see
            // https://developer.apple.com/fonts/TTRefMan/RM06/Chap6loca.html
            long endOfGlyphs = offsets[numGlyphs];
            long offset = getOffset();
            if (glyphs == null)
            {
                glyphs = new GlyphData[numGlyphs];
            }
         
            for (int gid = 0; gid < numGlyphs; gid++)
            {
                // end of glyphs reached?
                if (endOfGlyphs != 0 && endOfGlyphs == offsets[gid])
                {
                    break;
                }
                // the current glyph isn't defined
                // if the next offset is equal or smaller to the current offset
                if (offsets[gid + 1] <= offsets[gid])
                {
                    continue;
                }
                if (glyphs[gid] != null)
                {
                    // already cached
                    continue;
                }

                data.seek(offset + offsets[gid]);

                if (glyphs[gid] == null)
                {
                    ++cached;
                }
                glyphs[gid] = getGlyphData(gid);
            }
            initialized = true;
            return glyphs;
        }
    }

    /**
     * @param glyphsValue The glyphs to set.
     */
    public void setGlyphs(GlyphData[] glyphsValue)
    {
        glyphs = glyphsValue;
    }

    /**
     * TODO fontbox Added so that we can know whether a glyph
     * (or outline) exists, without bothering to create
     * glyph data.
     * 
     * @param gid GID
     * @throws IOException if the font cannot be read
     */
    public boolean hasGlyph(int gid) throws IOException
    {
        if (gid < 0 || gid >= numGlyphs)
        {
            return false;
        }
        
        synchronized (font)
        {
            // read a single glyph
            long[] offsets = loca.getOffsets();

            if (offsets[gid] == offsets[gid + 1])
            {
                // no outline
                return false;
            }
            
            return true;
        }
    }
    
    /**
     * Returns the data for the glyph with the given GID.
     *
     * @param gid GID
     * @throws IOException if the font cannot be read
     */
    public GlyphData getGlyph(int gid) throws IOException
    {
        if (gid < 0 || gid >= numGlyphs)
        {
            return null;
        }
        
        if (glyphs != null && glyphs[gid] != null)
        {
            return glyphs[gid];
        }

        synchronized (font)
        {
            // read a single glyph
            long[] offsets = loca.getOffsets();

            if (offsets[gid] == offsets[gid + 1])
            {
                // no outline
                return null;
            }
            
            // save
            long currentPosition = data.getCurrentPosition();

            data.seek(getOffset() + offsets[gid]);

            // TODO fbperf
            GlyphData glyph = getGlyphData(gid);

            // restore
            data.seek(currentPosition);

            if (glyphs != null && glyphs[gid] == null && cached < MAX_CACHED_GLYPHS)
            {
                glyphs[gid] = glyph;
                ++cached;
            }

            return glyph;
        }
    }

    private GlyphData getGlyphData(int gid) throws IOException
    {
        GlyphData glyph = new GlyphData();
        HorizontalMetricsTable hmt = font.getHorizontalMetrics();
        int leftSideBearing = hmt == null ? 0 : hmt.getLeftSideBearing(gid);
        // TODO fbperf
        glyph.initData(this, data, leftSideBearing);
        // resolve composite glyph
        if (glyph.getDescription().isComposite())
        {
            glyph.getDescription().resolve();
        }
        return glyph;
    }
}
