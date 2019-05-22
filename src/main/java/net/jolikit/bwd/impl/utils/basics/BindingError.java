/*
 * Copyright 2019 Jeff Hain
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
package net.jolikit.bwd.impl.utils.basics;

/**
 * Exception to indicate an error in binding code,
 * as opposite to an exception coming from user code.
 */
public class BindingError extends Error {
    
    private static final long serialVersionUID = 1L;
    
    public BindingError() {
    }
    
    public BindingError(String message) {
        super(message);
    }

    public BindingError(String message, Throwable cause) {
        super(message, cause);
    }

    public BindingError(Throwable cause) {
        super(cause);
    }
}
