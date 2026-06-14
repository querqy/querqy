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
package querqy.lucene.rewrite.cache;

import querqy.CharSequenceUtil;
import querqy.ComparableCharSequence;
import querqy.model.Term;

/**
 * @author rene
 *
 */
public class CacheKey {
    
    public final String fieldname;
   // public final Term term;
    protected final ComparableCharSequence value;
    
    public CacheKey(String fieldname, Term term) {
        this.fieldname = fieldname;
       // this.term = term;
        value = term.getValue();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((fieldname == null) ? 0 : fieldname.hashCode());
        result = prime * result + ((value == null) ? 0 : CharSequenceUtil.hashCode(value));
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
        CacheKey other = (CacheKey) obj;
        if (fieldname == null) {
            if (other.fieldname != null)
                return false;
        } else if (!fieldname.equals(other.fieldname))
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!CharSequenceUtil.equals(value, other.value))
            return false;
        return true;
    }

    
}
