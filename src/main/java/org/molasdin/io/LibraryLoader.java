/*
 * Copyright 2015 Bersenev Dmitry molasdin@outlook.com
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

package org.molasdin.io;

import java.net.URL;

/**
 * Created by molasdin on 3/18/15.
 */

/**
 * Provides required native library
 */
public class LibraryLoader {
    private String name;

    public LibraryLoader(String name) {
        this.name = name;
    }

    public void load(){
        String libFile = System.mapLibraryName(boundName());
        System.out.printf("Loading: %s\n", libFile);
        URL path = LibraryLoader.class.getClassLoader().getResource(libFile);
        if(path == null){
            throw new RuntimeException("Library not found at class path");
        }
        System.load(path.getFile());
    }

    private String boundName(){
        String arch = System.getProperty("os.arch");
        String osMarker = osMarker();
        return name.concat("_").concat(osMarker).concat("_").concat(arch);
    }

    private String osMarker(){
        String os = System.getProperty("os.name").toLowerCase();
        if(os.contains("win")){
            return "win32";
        }
        if(os.contains("nix")){
            return "linux";
        }
        if(os.contains("freebsd")){
            return "freebsd";
        }
        if(os.contains("mac")){
            return "darwin";
        }
        return "";
    }
}
