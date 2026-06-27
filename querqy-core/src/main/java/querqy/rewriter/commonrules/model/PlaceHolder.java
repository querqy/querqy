/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2015 Querqy Contributors
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
package querqy.rewriter.commonrules.model;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class PlaceHolder {
    
    public final int start;
    public final int length;
    public final int ref;
    
    public PlaceHolder(int start, int length, int ref) {
        this.start = start;
        this.length = length;
        this.ref = ref;
    }
    
    

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + length;
        result = prime * result + ref;
        result = prime * result + start;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PlaceHolder other = (PlaceHolder) obj;
        if (length != other.length)
            return false;
        if (ref != other.ref)
            return false;
        if (start != other.start)
            return false;
        return true;
    }



    @Override
    public String toString() {
        return "PlaceHolder [start=" + start + ", length=" + length + ", ref="
                + ref + "]";
    }

}
