/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

precision highp float;
   // uniform vec4 vColor;
    uniform sampler2D TexCoordIn;
    varying vec2 TexCoordOut;
    varying vec4 tposition;

    void main() {
        gl_FragColor = texture2D( TexCoordIn, vec2 (tposition.x, tposition.y));

        //gl_FragColor = texture2D( TexCoordIn, vec2 (TexCoordOut.x, TexCoordOut.y));
       // gl_FragColor = vec4(1.0 ,0.1 ,0.1 ,0.5);
    }