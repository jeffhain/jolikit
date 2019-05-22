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
/**
 * Package where to put sub-packages containing
 * internal APIs mimicking third parties libraries
 * we don't want to depend on.
 * 
 * We could also rework the reused code for it not to use the depended on
 * library, but it is must simpler to just refactor it to depend on our
 * dummy code, for example when updating with a newer version of
 * the reused code.
 */
package net.jolikit.internal.nodepto;
