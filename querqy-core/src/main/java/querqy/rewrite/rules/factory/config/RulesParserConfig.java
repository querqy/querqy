/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2021 Querqy Contributors
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
package querqy.rewrite.rules.factory.config;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NonNull;
import querqy.rewrite.commonrules.model.RulesCollectionBuilder;

@Builder
@Getter
public class RulesParserConfig {

    @Default private final TextParserConfig textParserConfig = TextParserConfig.defaultConfig();
    @Default private final RuleParserConfig ruleParserConfig = RuleParserConfig.defaultConfig();

    @NonNull private final RulesCollectionBuilder rulesCollectionBuilder;

}
