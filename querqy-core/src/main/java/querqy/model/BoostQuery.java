/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2014 Querqy Contributors
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
package querqy.model;

import lombok.EqualsAndHashCode;

/**
 * @author René Kriegler, @renekrie
 *
 */
@EqualsAndHashCode
public class BoostQuery {

   final QuerqyQuery<?> query;
   final float boost;

   public BoostQuery(QuerqyQuery<?> query, float boost) {

      this.boost = boost;
      this.query = query;

   }

   @Override
   public String toString() {
      return "BoostQuery [query=" + query + ", boost=" + boost + "]";
   }

   public float getBoost() {
      return boost;
   }

   public QuerqyQuery<?> getQuery() {
      return query;
   }

}
