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
package net.jolikit.bwd.test.utils;

import java.io.PrintStream;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import net.jolikit.lang.LangUtils;
import net.jolikit.lang.NbrsUtils;

/**
 * Simple helper class to deal with arguments
 * of the form "-a <x> <y> [-b <z>]" etc.
 */
public class ArgsHelper {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private final Map<String,Integer> argCountByKey = new TreeMap<String,Integer>();

    private final Set<String> mandatoryKeySet = new TreeSet<String>();

    private final Map<String,String> argDescrByKey = new TreeMap<String,String>();

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public ArgsHelper() {
    }
    
    /**
     * @param key A key preceding arguments, such as "-a" or "-foo".
     * @param argCount The expected number of arguments following the specified key.
     * @param mandatory True if the key (and its eventual arguments) are mandatory, false otherwise.
     * @param argDescr A description of the arguments, such as "<foo> <bar>".
     * @return The specified key (for convenience).
     * @throws NullPointerException if key or argDescr is null.
     * @throws IllegalArgumentException if the specified key has already been registered
     *         or if argNum is negative.
     */
    public String registedKey(String key, int argCount, boolean mandatory, String argDescr) {
        LangUtils.requireNonNull(key);
        LangUtils.requireNonNull(argDescr);
        NbrsUtils.requireSupOrEq(0, argCount, "argCount");
        if (this.argCountByKey.containsKey(key)) {
            throw new IllegalArgumentException("key " + key + " already registered");
        }
        
        this.argCountByKey.put(key, argCount);
        if (mandatory) {
            this.mandatoryKeySet.add(key);
        }
        this.argDescrByKey.put(key, argDescr);
        
        return key;
    }
    
    /**
     * Checks that the specified arguments contain all mandatory keys
     * and their eventual arguments, possible optional keys and their
     * eventual arguments, and no other key.
     * 
     * @param args Arguments to check.
     * @param stream Stream where to print error and usage in case of invalidity.
     *        Can be null, in which case nothing is printed.
     * @throws NullPointerException if args is null.
     * @throws IllegalArgumentException if the specified arguments are invalid.
     */
    public void checkArgs(String[] args, PrintStream stream) {
        
        final String errorMsg = computeErrorMsgIfInvalidArgs(args);
        
        if (errorMsg != null) {
            if (stream != null) {
                stream.println(errorMsg);
                this.printUsage(stream);
            }
            throw new IllegalArgumentException(errorMsg);
        }
    }
    
    /**
     * Public for use in case of further checks done outside this class.
     * 
     * @param stream The stream to print into.
     * @throws NullPointerException if stream is null.
     */
    public void printUsage(PrintStream stream) {
        LangUtils.requireNonNull(stream);
        
        /*
         * Just using using PrintStream.print(String),
         * for simplicity, so we build a string.
         */
        final StringBuilder sb = new StringBuilder();
        sb.append("Usage:");
        if (this.argDescrByKey.size() == 0) {
            sb.append(" (no args)");
        } else {
            for (Map.Entry<String,String> entry : this.argDescrByKey.entrySet()) {
                final String key = entry.getKey();
                final String argDescr = entry.getValue();
                final boolean mandatory = this.mandatoryKeySet.contains(key);
                sb.append(" ");
                if (!mandatory) {
                    sb.append("[");
                }
                sb.append(key);
                sb.append(" ");
                sb.append(argDescr);
                if (!mandatory) {
                    sb.append("]");
                }
            }
        }
        stream.println(sb.toString());
    }
    
    /**
     * @param args The arguments.
     * @param key A key.
     * @param argNum The number of the argument corresponding to the key
     *        that must be returned. Starts at 1.
     * @return The argNum'th argument corresponding to the specified key,
     *         or null if the key doesn't appear.
     * @throws NullPointerException if args or key is null.
     * @throws IllegalArgumentException if argNum is out of range,
     *         or args are found to be invalid.
     */
    public String getArgN(String[] args, String key, int argNum) {
        LangUtils.requireNonNull(args);
        LangUtils.requireNonNull(key);
        NbrsUtils.requireSup(0, argNum, "argNum");
        
        int i = 0;
        while (i < args.length) {
            final String tmpKey = args[i];
            final Integer argCountRef = this.argCountByKey.get(tmpKey);
            if (argCountRef == null) {
                throw new IllegalArgumentException("unknown key: " + tmpKey);
            }
            final int argCount = argCountRef.intValue();
            if (tmpKey.equals(key)) {
                NbrsUtils.requireInRange(1, argCount, argNum, "argNum");
                final int remainingCountPastKey = (args.length - 1) - i;
                if (remainingCountPastKey < argNum) {
                    throw new IllegalArgumentException(
                            "not enough arguments (key = " + key + ", argNum = " + argNum
                            + ", remaining = " + remainingCountPastKey + ")");
                }
                return args[i + argNum];
            } else {
                // Going to next key, if any.
                i += (argCount + 1);
            }
        }
        
        return null;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    /**
     * @param args Args to check.
     * @return Error message if any, else null.
     * @throws NullPointerException if args is null.
     */
    private String computeErrorMsgIfInvalidArgs(String[] args) {
        LangUtils.requireNonNull(args);
        
        final Set<String> keyToEncounterSet = new TreeSet<String>(this.mandatoryKeySet);
        
        String errorMsg = null;
        
        int i = 0;
        while (i < args.length) {
            final String key = args[i];
            final Integer argCountRef = this.argCountByKey.get(key);
            if (argCountRef == null) {
                errorMsg = "unknown key: " + key;
                break;
            }
            final int argCount = argCountRef.intValue();
            final int remainingCountPastKey = (args.length - 1) - i;
            if (remainingCountPastKey < argCount) {
                errorMsg = "not enough arguments (key = " + key
                        + ", argCount = " + argCount
                        + ", remaining = " + remainingCountPastKey + ")";
                break;
            }
            // Going to next key, if any.
            i += (argCount + 1);
        }
        
        if (errorMsg == null) {
            if (keyToEncounterSet.size() != 0) {
                errorMsg = "missing mandatory keys: " + keyToEncounterSet;
            }
        }
        
        return errorMsg;
    }
}
