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
package net.jolikit.bwd.api.graphics;

import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Convenience class to deal with colors, for people uncomfortable
 * with using raw 32 bits or 64 bits ARGB integers.
 * 
 * Contains default instances of well-known (i.e. named) colors.
 * 
 * Immutable.
 */
public final class BwdColor implements Comparable<BwdColor> {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final int DEFAULT_ALPHA_8 = 0xFF;
    private static final int DEFAULT_ALPHA_16 = 0xFFFF;
    private static final double DEFAULT_ALPHA_FP = 1.0;

    /*
     * Named colors.
     * 
     * A view on the map is public, so for consistency across JDKs and because
     * alphabetical order is much nicer to deal with, we use a sorted map.
     */
    
    private static final SortedMap<String,BwdColor> COLOR_BY_NAME = new TreeMap<String,BwdColor>();
    private static final SortedMap<String,BwdColor> COLOR_BY_NAME_UNMODIFIABLE = Collections.unmodifiableSortedMap(COLOR_BY_NAME);

    /*
     * Same colors as in javafx.scene.paint.Color.
     */
    
    /**
     * A fully transparent color with an ARGB value of #00000000.
     */
    public static final BwdColor TRANSPARENT = BwdColor.valueOfArgb32(0);

    /**
     * The color alice blue with an RGB value of #F0F8FF
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#F0F8FF;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor ALICEBLUE = newRegisteredColor("aliceblue", 0.9411765f, 0.972549f, 1.0f);

    /**
     * The color antique white with an RGB value of #FAEBD7
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FAEBD7;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor ANTIQUEWHITE = newRegisteredColor("antiquewhite", 0.98039216f, 0.92156863f, 0.84313726f);

    /**
     * The color aqua with an RGB value of #00FFFF
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#00FFFF;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor AQUA = newRegisteredColor("aqua", 0.0f, 1.0f, 1.0f);

    /**
     * The color aquamarine with an RGB value of #7FFFD4
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#7FFFD4;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor AQUAMARINE = newRegisteredColor("aquamarine", 0.49803922f, 1.0f, 0.83137256f);

    /**
     * The color azure with an RGB value of #F0FFFF
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#F0FFFF;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor AZURE = newRegisteredColor("azure", 0.9411765f, 1.0f, 1.0f);

    /**
     * The color beige with an RGB value of #F5F5DC
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#F5F5DC;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor BEIGE = newRegisteredColor("beige", 0.9607843f, 0.9607843f, 0.8627451f);

    /**
     * The color bisque with an RGB value of #FFE4C4
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFE4C4;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor BISQUE = newRegisteredColor("bisque", 1.0f, 0.89411765f, 0.76862746f);

    /**
     * The color black with an RGB value of #000000
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#000000;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor BLACK = newRegisteredColor("black", 0.0f, 0.0f, 0.0f);

    /**
     * The color blanched almond with an RGB value of #FFEBCD
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFEBCD;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor BLANCHEDALMOND = newRegisteredColor("blanchedalmond", 1.0f, 0.92156863f, 0.8039216f);

    /**
     * The color blue with an RGB value of #0000FF
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#0000FF;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor BLUE = newRegisteredColor("blue", 0.0f, 0.0f, 1.0f);

    /**
     * The color blue violet with an RGB value of #8A2BE2
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#8A2BE2;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor BLUEVIOLET = newRegisteredColor("blueviolet", 0.5411765f, 0.16862746f, 0.8862745f);

    /**
     * The color brown with an RGB value of #A52A2A
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#A52A2A;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor BROWN = newRegisteredColor("brown", 0.64705884f, 0.16470589f, 0.16470589f);

    /**
     * The color burly wood with an RGB value of #DEB887
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#DEB887;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor BURLYWOOD = newRegisteredColor("burlywood", 0.87058824f, 0.72156864f, 0.5294118f);

    /**
     * The color cadet blue with an RGB value of #5F9EA0
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#5F9EA0;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor CADETBLUE = newRegisteredColor("cadetblue", 0.37254903f, 0.61960787f, 0.627451f);

    /**
     * The color chartreuse with an RGB value of #7FFF00
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#7FFF00;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor CHARTREUSE = newRegisteredColor("chartreuse", 0.49803922f, 1.0f, 0.0f);

    /**
     * The color chocolate with an RGB value of #D2691E
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#D2691E;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor CHOCOLATE = newRegisteredColor("chocolate", 0.8235294f, 0.4117647f, 0.11764706f);

    /**
     * The color coral with an RGB value of #FF7F50
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FF7F50;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor CORAL = newRegisteredColor("coral", 1.0f, 0.49803922f, 0.3137255f);

    /**
     * The color cornflower blue with an RGB value of #6495ED
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#6495ED;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor CORNFLOWERBLUE = newRegisteredColor("cornflowerblue", 0.39215687f, 0.58431375f, 0.92941177f);

    /**
     * The color cornsilk with an RGB value of #FFF8DC
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFF8DC;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor CORNSILK = newRegisteredColor("cornsilk", 1.0f, 0.972549f, 0.8627451f);

    /**
     * The color crimson with an RGB value of #DC143C
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#DC143C;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor CRIMSON = newRegisteredColor("crimson", 0.8627451f, 0.078431375f, 0.23529412f);

    /**
     * The color cyan with an RGB value of #00FFFF
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#00FFFF;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor CYAN = newRegisteredColor("cyan", 0.0f, 1.0f, 1.0f);

    /**
     * The color dark blue with an RGB value of #00008B
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#00008B;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor DARKBLUE = newRegisteredColor("darkblue", 0.0f, 0.0f, 0.54509807f);

    /**
     * The color dark cyan with an RGB value of #008B8B
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#008B8B;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor DARKCYAN = newRegisteredColor("darkcyan", 0.0f, 0.54509807f, 0.54509807f);

    /**
     * The color dark goldenrod with an RGB value of #B8860B
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#B8860B;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor DARKGOLDENROD = newRegisteredColor("darkgoldenrod", 0.72156864f, 0.5254902f, 0.043137256f);

    /**
     * The color dark gray with an RGB value of #A9A9A9
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#A9A9A9;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor DARKGRAY = newRegisteredColor("darkgray", 0.6627451f, 0.6627451f, 0.6627451f);

    /**
     * The color dark green with an RGB value of #006400
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#006400;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor DARKGREEN = newRegisteredColor("darkgreen", 0.0f, 0.39215687f, 0.0f);

    /**
     * The color dark grey with an RGB value of #A9A9A9
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#A9A9A9;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor DARKGREY = registeredColor("darkgrey", DARKGRAY);

    /**
     * The color dark khaki with an RGB value of #BDB76B
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#BDB76B;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor DARKKHAKI = newRegisteredColor("darkkhaki", 0.7411765f, 0.7176471f, 0.41960785f);

    /**
     * The color dark magenta with an RGB value of #8B008B
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#8B008B;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor DARKMAGENTA = newRegisteredColor("darkmagenta", 0.54509807f, 0.0f, 0.54509807f);

    /**
     * The color dark olive green with an RGB value of #556B2F
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#556B2F;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor DARKOLIVEGREEN = newRegisteredColor("darkolivegreen", 0.33333334f, 0.41960785f, 0.18431373f);

    /**
     * The color dark orange with an RGB value of #FF8C00
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FF8C00;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor DARKORANGE = newRegisteredColor("darkorange", 1.0f, 0.54901963f, 0.0f);

    /**
     * The color dark orchid with an RGB value of #9932CC
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#9932CC;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor DARKORCHID = newRegisteredColor("darkorchid", 0.6f, 0.19607843f, 0.8f);

    /**
     * The color dark red with an RGB value of #8B0000
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#8B0000;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor DARKRED = newRegisteredColor("darkred", 0.54509807f, 0.0f, 0.0f);

    /**
     * The color dark salmon with an RGB value of #E9967A
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#E9967A;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor DARKSALMON = newRegisteredColor("darksalmon", 0.9137255f, 0.5882353f, 0.47843137f);

    /**
     * The color dark sea green with an RGB value of #8FBC8F
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#8FBC8F;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor DARKSEAGREEN = newRegisteredColor("darkseagreen", 0.56078434f, 0.7372549f, 0.56078434f);

    /**
     * The color dark slate blue with an RGB value of #483D8B
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#483D8B;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor DARKSLATEBLUE = newRegisteredColor("darkslateblue", 0.28235295f, 0.23921569f, 0.54509807f);

    /**
     * The color dark slate gray with an RGB value of #2F4F4F
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#2F4F4F;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor DARKSLATEGRAY = newRegisteredColor("darkslategray", 0.18431373f, 0.30980393f, 0.30980393f);

    /**
     * The color dark slate grey with an RGB value of #2F4F4F
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#2F4F4F;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor DARKSLATEGREY = registeredColor("darkslategrey", DARKSLATEGRAY);

    /**
     * The color dark turquoise with an RGB value of #00CED1
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#00CED1;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor DARKTURQUOISE = newRegisteredColor("darkturquoise", 0.0f, 0.80784315f, 0.81960785f);

    /**
     * The color dark violet with an RGB value of #9400D3
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#9400D3;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor DARKVIOLET = newRegisteredColor("darkviolet", 0.5803922f, 0.0f, 0.827451f);

    /**
     * The color deep pink with an RGB value of #FF1493
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FF1493;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor DEEPPINK = newRegisteredColor("deeppink", 1.0f, 0.078431375f, 0.5764706f);

    /**
     * The color deep sky blue with an RGB value of #00BFFF
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#00BFFF;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor DEEPSKYBLUE = newRegisteredColor("deepskyblue", 0.0f, 0.7490196f, 1.0f);

    /**
     * The color dim gray with an RGB value of #696969
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#696969;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor DIMGRAY = newRegisteredColor("dimgray", 0.4117647f, 0.4117647f, 0.4117647f);

    /**
     * The color dim grey with an RGB value of #696969
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#696969;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor DIMGREY = registeredColor("dimgrey", DIMGRAY);

    /**
     * The color dodger blue with an RGB value of #1E90FF
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#1E90FF;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor DODGERBLUE = newRegisteredColor("dodgerblue", 0.11764706f, 0.5647059f, 1.0f);

    /**
     * The color firebrick with an RGB value of #B22222
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#B22222;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor FIREBRICK = newRegisteredColor("firebrick", 0.69803923f, 0.13333334f, 0.13333334f);

    /**
     * The color floral white with an RGB value of #FFFAF0
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFFAF0;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor FLORALWHITE = newRegisteredColor("floralwhite", 1.0f, 0.98039216f, 0.9411765f);

    /**
     * The color forest green with an RGB value of #228B22
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#228B22;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor FORESTGREEN = newRegisteredColor("forestgreen", 0.13333334f, 0.54509807f, 0.13333334f);

    /**
     * The color fuchsia with an RGB value of #FF00FF
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FF00FF;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor FUCHSIA = newRegisteredColor("fuchsia", 1.0f, 0.0f, 1.0f);

    /**
     * The color gainsboro with an RGB value of #DCDCDC
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#DCDCDC;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor GAINSBORO = newRegisteredColor("gainsboro", 0.8627451f, 0.8627451f, 0.8627451f);

    /**
     * The color ghost white with an RGB value of #F8F8FF
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#F8F8FF;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor GHOSTWHITE = newRegisteredColor("ghostwhite", 0.972549f, 0.972549f, 1.0f);

    /**
     * The color gold with an RGB value of #FFD700
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFD700;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor GOLD = newRegisteredColor("gold", 1.0f, 0.84313726f, 0.0f);

    /**
     * The color goldenrod with an RGB value of #DAA520
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#DAA520;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor GOLDENROD = newRegisteredColor("goldenrod", 0.85490197f, 0.64705884f, 0.1254902f);

    /**
     * The color gray with an RGB value of #808080
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#808080;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor GRAY = newRegisteredColor("gray", 0.5019608f, 0.5019608f, 0.5019608f);

    /**
     * The color green with an RGB value of #008000
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#008000;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor GREEN = newRegisteredColor("green", 0.0f, 0.5019608f, 0.0f);

    /**
     * The color green yellow with an RGB value of #ADFF2F
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#ADFF2F;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor GREENYELLOW = newRegisteredColor("greenyellow", 0.6784314f, 1.0f, 0.18431373f);

    /**
     * The color grey with an RGB value of #808080
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#808080;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor GREY = registeredColor("grey", GRAY);

    /**
     * The color honeydew with an RGB value of #F0FFF0
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#F0FFF0;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor HONEYDEW = newRegisteredColor("honeydew", 0.9411765f, 1.0f, 0.9411765f);

    /**
     * The color hot pink with an RGB value of #FF69B4
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FF69B4;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor HOTPINK = newRegisteredColor("hotpink", 1.0f, 0.4117647f, 0.7058824f);

    /**
     * The color indian red with an RGB value of #CD5C5C
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#CD5C5C;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor INDIANRED = newRegisteredColor("indianred", 0.8039216f, 0.36078432f, 0.36078432f);

    /**
     * The color indigo with an RGB value of #4B0082
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#4B0082;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor INDIGO = newRegisteredColor("indigo", 0.29411766f, 0.0f, 0.50980395f);

    /**
     * The color ivory with an RGB value of #FFFFF0
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFFFF0;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor IVORY = newRegisteredColor("ivory", 1.0f, 1.0f, 0.9411765f);

    /**
     * The color khaki with an RGB value of #F0E68C
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#F0E68C;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor KHAKI = newRegisteredColor("khaki", 0.9411765f, 0.9019608f, 0.54901963f);

    /**
     * The color lavender with an RGB value of #E6E6FA
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#E6E6FA;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor LAVENDER = newRegisteredColor("lavender", 0.9019608f, 0.9019608f, 0.98039216f);

    /**
     * The color lavender blush with an RGB value of #FFF0F5
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFF0F5;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor LAVENDERBLUSH = newRegisteredColor("lavenderblush", 1.0f, 0.9411765f, 0.9607843f);

    /**
     * The color lawn green with an RGB value of #7CFC00
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#7CFC00;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor LAWNGREEN = newRegisteredColor("lawngreen", 0.4862745f, 0.9882353f, 0.0f);

    /**
     * The color lemon chiffon with an RGB value of #FFFACD
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFFACD;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor LEMONCHIFFON = newRegisteredColor("lemonchiffon", 1.0f, 0.98039216f, 0.8039216f);

    /**
     * The color light blue with an RGB value of #ADD8E6
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#ADD8E6;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor LIGHTBLUE = newRegisteredColor("lightblue", 0.6784314f, 0.84705883f, 0.9019608f);

    /**
     * The color light coral with an RGB value of #F08080
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#F08080;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor LIGHTCORAL = newRegisteredColor("lightcoral", 0.9411765f, 0.5019608f, 0.5019608f);

    /**
     * The color light cyan with an RGB value of #E0FFFF
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#E0FFFF;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor LIGHTCYAN = newRegisteredColor("lightcyan", 0.8784314f, 1.0f, 1.0f);

    /**
     * The color light goldenrod yellow with an RGB value of #FAFAD2
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FAFAD2;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor LIGHTGOLDENRODYELLOW = newRegisteredColor("lightgoldenrodyellow", 0.98039216f, 0.98039216f, 0.8235294f);

    /**
     * The color light gray with an RGB value of #D3D3D3
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#D3D3D3;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor LIGHTGRAY = newRegisteredColor("lightgray", 0.827451f, 0.827451f, 0.827451f);

    /**
     * The color light green with an RGB value of #90EE90
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#90EE90;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor LIGHTGREEN = newRegisteredColor("lightgreen", 0.5647059f, 0.93333334f, 0.5647059f);

    /**
     * The color light grey with an RGB value of #D3D3D3
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#D3D3D3;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor LIGHTGREY = registeredColor("lightgrey", LIGHTGRAY);

    /**
     * The color light pink with an RGB value of #FFB6C1
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFB6C1;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor LIGHTPINK = newRegisteredColor("lightpink", 1.0f, 0.7137255f, 0.75686276f);

    /**
     * The color light salmon with an RGB value of #FFA07A
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFA07A;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor LIGHTSALMON = newRegisteredColor("lightsalmon", 1.0f, 0.627451f, 0.47843137f);

    /**
     * The color light sea green with an RGB value of #20B2AA
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#20B2AA;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor LIGHTSEAGREEN = newRegisteredColor("lightseagreen", 0.1254902f, 0.69803923f, 0.6666667f);

    /**
     * The color light sky blue with an RGB value of #87CEFA
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#87CEFA;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor LIGHTSKYBLUE = newRegisteredColor("lightskyblue", 0.5294118f, 0.80784315f, 0.98039216f);

    /**
     * The color light slate gray with an RGB value of #778899
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#778899;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor LIGHTSLATEGRAY = newRegisteredColor("lightslategray", 0.46666667f, 0.53333336f, 0.6f);

    /**
     * The color light slate grey with an RGB value of #778899
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#778899;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor LIGHTSLATEGREY = registeredColor("lightslategrey", LIGHTSLATEGRAY);

    /**
     * The color light steel blue with an RGB value of #B0C4DE
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#B0C4DE;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor LIGHTSTEELBLUE = newRegisteredColor("lightsteelblue", 0.6901961f, 0.76862746f, 0.87058824f);

    /**
     * The color light yellow with an RGB value of #FFFFE0
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFFFE0;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor LIGHTYELLOW = newRegisteredColor("lightyellow", 1.0f, 1.0f, 0.8784314f);

    /**
     * The color lime with an RGB value of #00FF00
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#00FF00;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor LIME = newRegisteredColor("lime", 0.0f, 1.0f, 0.0f);

    /**
     * The color lime green with an RGB value of #32CD32
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#32CD32;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor LIMEGREEN = newRegisteredColor("limegreen", 0.19607843f, 0.8039216f, 0.19607843f);

    /**
     * The color linen with an RGB value of #FAF0E6
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FAF0E6;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor LINEN = newRegisteredColor("linen", 0.98039216f, 0.9411765f, 0.9019608f);

    /**
     * The color magenta with an RGB value of #FF00FF
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FF00FF;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor MAGENTA = newRegisteredColor("magenta", 1.0f, 0.0f, 1.0f);

    /**
     * The color maroon with an RGB value of #800000
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#800000;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor MAROON = newRegisteredColor("maroon", 0.5019608f, 0.0f, 0.0f);

    /**
     * The color medium aquamarine with an RGB value of #66CDAA
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#66CDAA;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor MEDIUMAQUAMARINE = newRegisteredColor("mediumaquamarine", 0.4f, 0.8039216f, 0.6666667f);

    /**
     * The color medium blue with an RGB value of #0000CD
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#0000CD;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor MEDIUMBLUE = newRegisteredColor("mediumblue", 0.0f, 0.0f, 0.8039216f);

    /**
     * The color medium orchid with an RGB value of #BA55D3
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#BA55D3;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor MEDIUMORCHID = newRegisteredColor("mediumorchid", 0.7294118f, 0.33333334f, 0.827451f);

    /**
     * The color medium purple with an RGB value of #9370DB
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#9370DB;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor MEDIUMPURPLE = newRegisteredColor("mediumpurple", 0.5764706f, 0.4392157f, 0.85882354f);

    /**
     * The color medium sea green with an RGB value of #3CB371
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#3CB371;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor MEDIUMSEAGREEN = newRegisteredColor("mediumseagreen", 0.23529412f, 0.7019608f, 0.44313726f);

    /**
     * The color medium slate blue with an RGB value of #7B68EE
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#7B68EE;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor MEDIUMSLATEBLUE = newRegisteredColor("mediumslateblue", 0.48235294f, 0.40784314f, 0.93333334f);

    /**
     * The color medium spring green with an RGB value of #00FA9A
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#00FA9A;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor MEDIUMSPRINGGREEN = newRegisteredColor("mediumspringgreen", 0.0f, 0.98039216f, 0.6039216f);

    /**
     * The color medium turquoise with an RGB value of #48D1CC
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#48D1CC;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor MEDIUMTURQUOISE = newRegisteredColor("mediumturquoise", 0.28235295f, 0.81960785f, 0.8f);

    /**
     * The color medium violet red with an RGB value of #C71585
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#C71585;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor MEDIUMVIOLETRED = newRegisteredColor("mediumvioletred", 0.78039217f, 0.08235294f, 0.52156866f);

    /**
     * The color midnight blue with an RGB value of #191970
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#191970;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor MIDNIGHTBLUE = newRegisteredColor("midnightblue", 0.09803922f, 0.09803922f, 0.4392157f);

    /**
     * The color mint cream with an RGB value of #F5FFFA
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#F5FFFA;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor MINTCREAM = newRegisteredColor("mintcream", 0.9607843f, 1.0f, 0.98039216f);

    /**
     * The color misty rose with an RGB value of #FFE4E1
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFE4E1;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor MISTYROSE = newRegisteredColor("mistyrose", 1.0f, 0.89411765f, 0.88235295f);

    /**
     * The color moccasin with an RGB value of #FFE4B5
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFE4B5;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor MOCCASIN = newRegisteredColor("moccasin", 1.0f, 0.89411765f, 0.70980394f);

    /**
     * The color navajo white with an RGB value of #FFDEAD
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFDEAD;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor NAVAJOWHITE = newRegisteredColor("navajowhite", 1.0f, 0.87058824f, 0.6784314f);

    /**
     * The color navy with an RGB value of #000080
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#000080;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor NAVY = newRegisteredColor("navy", 0.0f, 0.0f, 0.5019608f);

    /**
     * The color old lace with an RGB value of #FDF5E6
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FDF5E6;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor OLDLACE = newRegisteredColor("oldlace", 0.99215686f, 0.9607843f, 0.9019608f);

    /**
     * The color olive with an RGB value of #808000
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#808000;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor OLIVE = newRegisteredColor("olive", 0.5019608f, 0.5019608f, 0.0f);

    /**
     * The color olive drab with an RGB value of #6B8E23
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#6B8E23;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor OLIVEDRAB = newRegisteredColor("olivedrab", 0.41960785f, 0.5568628f, 0.13725491f);

    /**
     * The color orange with an RGB value of #FFA500
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFA500;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor ORANGE = newRegisteredColor("orange", 1.0f, 0.64705884f, 0.0f);

    /**
     * The color orange red with an RGB value of #FF4500
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FF4500;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor ORANGERED = newRegisteredColor("orangered", 1.0f, 0.27058825f, 0.0f);

    /**
     * The color orchid with an RGB value of #DA70D6
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#DA70D6;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor ORCHID = newRegisteredColor("orchid", 0.85490197f, 0.4392157f, 0.8392157f);

    /**
     * The color pale goldenrod with an RGB value of #EEE8AA
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#EEE8AA;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor PALEGOLDENROD = newRegisteredColor("palegoldenrod", 0.93333334f, 0.9098039f, 0.6666667f);

    /**
     * The color pale green with an RGB value of #98FB98
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#98FB98;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor PALEGREEN = newRegisteredColor("palegreen", 0.59607846f, 0.9843137f, 0.59607846f);

    /**
     * The color pale turquoise with an RGB value of #AFEEEE
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#AFEEEE;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor PALETURQUOISE = newRegisteredColor("paleturquoise", 0.6862745f, 0.93333334f, 0.93333334f);

    /**
     * The color pale violet red with an RGB value of #DB7093
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#DB7093;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor PALEVIOLETRED = newRegisteredColor("palevioletred", 0.85882354f, 0.4392157f, 0.5764706f);

    /**
     * The color papaya whip with an RGB value of #FFEFD5
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFEFD5;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor PAPAYAWHIP = newRegisteredColor("papayawhip", 1.0f, 0.9372549f, 0.8352941f);

    /**
     * The color peach puff with an RGB value of #FFDAB9
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFDAB9;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor PEACHPUFF = newRegisteredColor("peachpuff", 1.0f, 0.85490197f, 0.7254902f);

    /**
     * The color peru with an RGB value of #CD853F
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#CD853F;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor PERU = newRegisteredColor("peru", 0.8039216f, 0.52156866f, 0.24705882f);

    /**
     * The color pink with an RGB value of #FFC0CB
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFC0CB;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor PINK = newRegisteredColor("pink", 1.0f, 0.7529412f, 0.79607844f);

    /**
     * The color plum with an RGB value of #DDA0DD
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#DDA0DD;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor PLUM = newRegisteredColor("plum", 0.8666667f, 0.627451f, 0.8666667f);

    /**
     * The color powder blue with an RGB value of #B0E0E6
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#B0E0E6;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor POWDERBLUE = newRegisteredColor("powderblue", 0.6901961f, 0.8784314f, 0.9019608f);

    /**
     * The color purple with an RGB value of #800080
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#800080;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor PURPLE = newRegisteredColor("purple", 0.5019608f, 0.0f, 0.5019608f);

    /**
     * The color red with an RGB value of #FF0000
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FF0000;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor RED = newRegisteredColor("red", 1.0f, 0.0f, 0.0f);

    /**
     * The color rosy brown with an RGB value of #BC8F8F
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#BC8F8F;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor ROSYBROWN = newRegisteredColor("rosybrown", 0.7372549f, 0.56078434f, 0.56078434f);

    /**
     * The color royal blue with an RGB value of #4169E1
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#4169E1;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor ROYALBLUE = newRegisteredColor("royalblue", 0.25490198f, 0.4117647f, 0.88235295f);

    /**
     * The color saddle brown with an RGB value of #8B4513
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#8B4513;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor SADDLEBROWN = newRegisteredColor("saddlebrown", 0.54509807f, 0.27058825f, 0.07450981f);

    /**
     * The color salmon with an RGB value of #FA8072
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FA8072;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor SALMON = newRegisteredColor("salmon", 0.98039216f, 0.5019608f, 0.44705883f);

    /**
     * The color sandy brown with an RGB value of #F4A460
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#F4A460;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor SANDYBROWN = newRegisteredColor("sandybrown", 0.95686275f, 0.6431373f, 0.3764706f);

    /**
     * The color sea green with an RGB value of #2E8B57
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#2E8B57;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor SEAGREEN = newRegisteredColor("seagreen", 0.18039216f, 0.54509807f, 0.34117648f);

    /**
     * The color sea shell with an RGB value of #FFF5EE
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFF5EE;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor SEASHELL = newRegisteredColor("seashell", 1.0f, 0.9607843f, 0.93333334f);

    /**
     * The color sienna with an RGB value of #A0522D
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#A0522D;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor SIENNA = newRegisteredColor("sienna", 0.627451f, 0.32156864f, 0.1764706f);

    /**
     * The color silver with an RGB value of #C0C0C0
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#C0C0C0;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor SILVER = newRegisteredColor("silver", 0.7529412f, 0.7529412f, 0.7529412f);

    /**
     * The color sky blue with an RGB value of #87CEEB
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#87CEEB;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor SKYBLUE = newRegisteredColor("skyblue", 0.5294118f, 0.80784315f, 0.92156863f);

    /**
     * The color slate blue with an RGB value of #6A5ACD
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#6A5ACD;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor SLATEBLUE = newRegisteredColor("slateblue", 0.41568628f, 0.3529412f, 0.8039216f);

    /**
     * The color slate gray with an RGB value of #708090
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#708090;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor SLATEGRAY = newRegisteredColor("slategray", 0.4392157f, 0.5019608f, 0.5647059f);

    /**
     * The color slate grey with an RGB value of #708090
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#708090;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor SLATEGREY = registeredColor("slategrey", SLATEGRAY);

    /**
     * The color snow with an RGB value of #FFFAFA
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFFAFA;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor SNOW = newRegisteredColor("snow", 1.0f, 0.98039216f, 0.98039216f);

    /**
     * The color spring green with an RGB value of #00FF7F
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#00FF7F;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor SPRINGGREEN = newRegisteredColor("springgreen", 0.0f, 1.0f, 0.49803922f);

    /**
     * The color steel blue with an RGB value of #4682B4
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#4682B4;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor STEELBLUE = newRegisteredColor("steelblue", 0.27450982f, 0.50980395f, 0.7058824f);

    /**
     * The color tan with an RGB value of #D2B48C
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#D2B48C;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor TAN = newRegisteredColor("tan", 0.8235294f, 0.7058824f, 0.54901963f);

    /**
     * The color teal with an RGB value of #008080
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#008080;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor TEAL = newRegisteredColor("teal", 0.0f, 0.5019608f, 0.5019608f);

    /**
     * The color thistle with an RGB value of #D8BFD8
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#D8BFD8;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor THISTLE = newRegisteredColor("thistle", 0.84705883f, 0.7490196f, 0.84705883f);

    /**
     * The color tomato with an RGB value of #FF6347
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FF6347;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor TOMATO = newRegisteredColor("tomato", 1.0f, 0.3882353f, 0.2784314f);

    /**
     * The color turquoise with an RGB value of #40E0D0
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#40E0D0;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor TURQUOISE = newRegisteredColor("turquoise", 0.2509804f, 0.8784314f, 0.8156863f);

    /**
     * The color violet with an RGB value of #EE82EE
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#EE82EE;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor VIOLET = newRegisteredColor("violet", 0.93333334f, 0.50980395f, 0.93333334f);

    /**
     * The color wheat with an RGB value of #F5DEB3
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#F5DEB3;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor WHEAT = newRegisteredColor("wheat", 0.9607843f, 0.87058824f, 0.7019608f);

    /**
     * The color white with an RGB value of #FFFFFF
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFFFFF;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor WHITE = newRegisteredColor("white", 1.0f, 1.0f, 1.0f);

    /**
     * The color white smoke with an RGB value of #F5F5F5
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#F5F5F5;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor WHITESMOKE = newRegisteredColor("whitesmoke", 0.9607843f, 0.9607843f, 0.9607843f);

    /**
     * The color yellow with an RGB value of #FFFF00
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFFF00;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor YELLOW = newRegisteredColor("yellow", 1.0f, 1.0f, 0.0f);

    /**
     * The color yellow green with an RGB value of #9ACD32
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#9ACD32;float:right;margin: 0 10px 0 0"></div><br/><br/>
     */
    public static final BwdColor YELLOWGREEN = newRegisteredColor("yellowgreen", 0.6039216f, 0.8039216f, 0.19607843f);

    /*
     * 
     */
    
    private final long argb64;
    
    /**
     * To make toArgb32() fast.
     */
    private final int argb32;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /*
     * Methods to retrieve named colors.
     */
    
    /**
     * Colors names are lower-case of static color fields names.
     * 
     * @return An unmodifiable map containing named colors by name.
     */
    public static Map<String,BwdColor> colorByName() {
        return COLOR_BY_NAME_UNMODIFIABLE;
    }
    
    /*
     * Construction.
     */

    /**
     * @param argb32 A 32 bits ARGB color.
     * @return A color object corresponding to the specified value.
     */
    public static BwdColor valueOfArgb32(int argb32) {
        return new BwdColor(
                Argb3264.toArgb64(argb32),
                argb32);
    }
    
    /**
     * @param argb64 A 64 bits ARGB color.
     * @return A color object corresponding to the specified value.
     */
    public static BwdColor valueOfArgb64(long argb64) {
        return new BwdColor(
                argb64,
                Argb3264.toArgb32(argb64));
    }
    
    /*
     * 
     */

    /**
     * @param red8 Red component, in [0,255].
     * @param green8 Green component, in [0,255].
     * @param blue8 Blue component, in [0,255].
     * @return An opaque color corresponding to the specified values.
     * @throws IllegalArgumentException if any value is out of range.
     */
    public static BwdColor valueOfRgb8(int red8, int green8, int blue8) {
        final int argb32 = Argb32.toArgb32FromInt8(DEFAULT_ALPHA_8, red8, green8, blue8);
        final long argb64 = Argb3264.toArgb64(argb32);
        return valueOfArgb64(argb64);
    }

    /**
     * @param red16 Red component, in [0,65535].
     * @param green16 Green component, in [0,65535].
     * @param blue16 Blue component, in [0,65535].
     * @return An opaque color corresponding to the specified values.
     * @throws IllegalArgumentException if any value is out of range.
     */
    public static BwdColor valueOfRgb16(int red16, int green16, int blue16) {
        final long argb64 = Argb64.toArgb64FromInt16(DEFAULT_ALPHA_16, red16, green16, blue16);
        return valueOfArgb64(argb64);
    }

    /**
     * @param redFp Red component, in [0,1].
     * @param greenFp Green component, in [0,1].
     * @param blueFp Blue component, in [0,1].
     * @return An opaque color corresponding to the specified values.
     * @throws IllegalArgumentException if any value is out of range.
     */
    public static BwdColor valueOfRgbFp(double redFp, double greenFp, double blueFp) {
        checkRgbFp(redFp, greenFp, blueFp);
        return valueOfArgb64(toArgb64FromArgbFp_noCheck(DEFAULT_ALPHA_FP, redFp, greenFp, blueFp));
    }

    /*
     * 
     */
    
    /**
     * @param red8 Red component, in [0,255].
     * @param green8 Green component, in [0,255].
     * @param blue8 Blue component, in [0,255].
     * @param alphaFp Alpha component, in [0,1].
     * @return A color corresponding to the specified values.
     * @throws IllegalArgumentException if any value is out of range.
     */
    public static BwdColor valueOfAFpRgb8(double alphaFp, int red8, int green8, int blue8) {
        final int alpha16 = Argb64.toInt16FromFp(alphaFp);
        final int red16 = Argb3264.toInt16FromInt8(red8);
        final int green16 = Argb3264.toInt16FromInt8(green8);
        final int blue16 = Argb3264.toInt16FromInt8(blue8);
        final long argb64 = Argb64.toArgb64FromInt16(alpha16, red16, green16, blue16);
        return valueOfArgb64(argb64);
    }

    /**
     * @param red16 Red component, in [0,65535].
     * @param green16 Green component, in [0,65535].
     * @param blue16 Blue component, in [0,65535].
     * @param alphaFp Alpha component, in [0,1].
     * @return A color corresponding to the specified values.
     * @throws IllegalArgumentException if any value is out of range.
     */
    public static BwdColor valueOfAFpRgb16(double alphaFp, int red16, int green16, int blue16) {
        final int alpha16 = Argb64.toInt16FromFp(alphaFp);
        final long argb64 = Argb64.toArgb64FromInt16(alpha16, red16, green16, blue16);
        return valueOfArgb64(argb64);
    }
    
    /**
     * Not calling it "valueOfArgbFp" for consistency with other methods.
     * 
     * @param redFp Red component, in [0,1].
     * @param greenFp Green component, in [0,1].
     * @param blueFp Blue component, in [0,1].
     * @param alphaFp Alpha component, in [0,1].
     * @return A color corresponding to the specified values.
     * @throws IllegalArgumentException if any value is out of range.
     */
    public static BwdColor valueOfAFpRgbFp(double alphaFp, double redFp, double greenFp, double blueFp) {
        checkAlphaFp(alphaFp);
        checkRgbFp(redFp, greenFp, blueFp);
        return valueOfArgb64(toArgb64FromArgbFp_noCheck(alphaFp, redFp, greenFp, blueFp));
    }

    /*
     * Derivation.
     */
    
    /**
     * @param alphaFp Alpha component in [0,1].
     * @return A color with same RGB as this color but with
     *         the specified alpha.
     * @throws IllegalArgumentException if alphaFp is out of range.
     */
    public BwdColor withAlphaFp(double alphaFp) {
        final long newArgb64 = Argb64.withAlphaFp(this.argb64, alphaFp);
        if (newArgb64 == this.argb64) {
            return this;
        }
        return valueOfArgb64(newArgb64);
    }

    /**
     * @return A color with same alpha as this color but with
     *         each other component replaced with (max - cpt).
     */
    public BwdColor inverted() {
        final long invertedArgb64 = Argb64.inverted(this.argb64);
        return valueOfArgb64(invertedArgb64);
    }

    /**
     * @param c Color to interpolate with.
     * @param t Interpolation ratio, in [0,1].
     * @return A color which each component, alpha included,
     *         is interpolated as "this_cpt * (1-t) + c_cpt * t".
     * @throws NullPointerException if the specified color is null.
     * @throws IllegalArgumentException if t is out of range.
     */
    public BwdColor interpolated(BwdColor c, double t) {
        // Implicit null check.
        final long interpolatedArgb64 = Argb64.interpolated(this.argb64, c.argb64, t);
        return valueOfArgb64(interpolatedArgb64);
    }

    /*
     * Generic.
     */
    
    /**
     * @return A string of the form 0xAAAARRRRGGGGBBBB.
     */
    @Override
    public String toString() {
        return Argb64.toString(this.argb64);
    }

    @Override
    public int hashCode() {
        /*
         * Here, JavaFX uses alpha in LSBits, whereas we use it in MSBits.
         * 
         * This should be better (colors having usually the same alpha),
         * since hashCode() usages should more easily ignore MSBits than LSBits.
         * 
         * As Doug said (http://mail.openjdk.java.net/pipermail/core-libs-dev/2014-July/027702.html):
         * "Bear in mind that the number of bits of identityHashCode is less
         * than 32 on all JVMs I know. It can be as low as 23, which means that
         * you are sure to see a lot of exact collisions on IHMs with only
         * tens of millions of elements, and there's nothing much you can
         * do that will help."
         */
        return this.toArgb32();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof BwdColor)) {
            return false;
        }
        final BwdColor other = (BwdColor) obj;
        return this.argb64 == other.argb64;
    }

    /**
     * Uses the natural ordering of the 64 bits ARGB integer.
     */
    @Override
    public int compareTo(BwdColor other) {
        if (this.argb64 < other.argb64) {
            return -1;
        } else if (this.argb64 > other.argb64) {
            return 1;
        } else {
            return 0;
        }
    }
    
    /*
     * Getters.
     */
    
    /**
     * @return The corresponding 32 bits ARGB.
     */
    public int toArgb32() {
        return this.argb32;
    }

    /**
     * @return The corresponding 64 bits ARGB.
     */
    public long toArgb64() {
        return this.argb64;
    }

    /*
     * 
     */
    
    /**
     * @return The alpha component in [0,255].
     */
    public int getAlpha8() {
        return Argb64.getAlpha8(this.argb64);
    }

    /**
     * @return The red component in [0,255].
     */
    public int getRed8() {
        return Argb64.getRed8(this.argb64);
    }

    /**
     * @return The green component in [0,255].
     */
    public int getGreen8() {
        return Argb64.getGreen8(this.argb64);
    }

    /**
     * @return The blue component in [0,255].
     */
    public int getBlue8() {
        return Argb64.getBlue8(this.argb64);
    }

    /*
     * 
     */
    
    /**
     * @return The alpha component in [0,65535].
     */
    public int getAlpha16() {
        return Argb64.getAlpha16(this.argb64);
    }

    /**
     * @return The red component in [0,65535].
     */
    public int getRed16() {
        return Argb64.getRed16(this.argb64);
    }

    /**
     * @return The green component in [0,65535].
     */
    public int getGreen16() {
        return Argb64.getGreen16(this.argb64);
    }

    /**
     * @return The blue component in [0,65535].
     */
    public int getBlue16() {
        return Argb64.getBlue16(this.argb64);
    }
    
    /*
     * 
     */

    /**
     * @return The alpha component in [0,1].
     */
    public double getAlphaFp() {
        return Argb64.getAlphaFp(this.argb64);
    }

    /**
     * @return The red component in [0,1].
     */
    public double getRedFp() {
        return Argb64.getRedFp(this.argb64);
    }

    /**
     * @return The green component in [0,1].
     */
    public double getGreenFp() {
        return Argb64.getGreenFp(this.argb64);
    }

    /**
     * @return The blue component in [0,1].
     */
    public double getBlueFp() {
        return Argb64.getBlueFp(this.argb64);
    }
    
    /*
     * Computations.
     */
    
    /**
     * @return True if this color is opaque,
     *         i.e. if its floating point alpha component is 1,
     *         false otherwise.
     */
    public boolean isOpaque() {
        return Argb64.isOpaque(this.argb64);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private BwdColor(
            long argb64,
            int argb32) {
        this.argb64 = argb64;
        this.argb32 = argb32;
    }

    /*
     * Named colors.
     */
    
    /**
     * Creates, registers and returns a corresponding color.
     */
    private static BwdColor newRegisteredColor(String name, float redFp, float greenFp, float blueFp) {
        return registeredColor(name, BwdColor.valueOfRgbFp(redFp, greenFp, blueFp));
    }
    
    /**
     * Registers and returns the specified color.
     */
    private static BwdColor registeredColor(String name, BwdColor color) {
        COLOR_BY_NAME.put(name, color);
        return color;
    }
    
    /*
     * Checks.
     */
    
    /**
     * @param name Value name, for exception message.
     */
    private static void check_0_1(String name, double value) {
        // Rejects NaNs.
        if (!((value >= 0.0) && (value <= 1.0))) {
            throw new IllegalArgumentException(name + " [" + value + "] must be in [0,1]");
        }
    }
    
    private static void checkRgbFp(double redFp, double greenFp, double blueFp) {
        check_0_1("redFp", redFp);
        check_0_1("greenFp", greenFp);
        check_0_1("blueFp", blueFp);
    }
    
    private static void checkAlphaFp(double alphaFp) {
        check_0_1("alphaFp", alphaFp);
    }
    
    /*
     * 
     */

    private static long toArgb64FromArgbFp_noCheck(
            double alphaFp, double redFp, double greenFp, double blueFp) {
        return Argb64.toArgb64FromInt16_noCheck(
                Argb64.toInt16FromFp_noCheck(alphaFp),
                Argb64.toInt16FromFp_noCheck(redFp),
                Argb64.toInt16FromFp_noCheck(greenFp),
                Argb64.toInt16FromFp_noCheck(blueFp));
    }
}
