/*
 * Copyright 2020 Jeff Hain
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
/**
 * BWD extensions, depending on BWD API,
 * and that can be useful both for BWD tests
 * and for toolkits created on top of BWD API.
 * 
 * Not using "Bwd" key work in the naming of these classes,
 * because they are not part of the essence of BWD which is
 * its API, and toolkits implementations might choose to use
 * different extensions: for users of toolkits depending on BWD,
 * that would make these classes wrongly look legitimately applicable.
 */
package net.jolikit.bwd.ext;
