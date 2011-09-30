/*
 * Copyright 2011 Glenn Heinze <glenn@gheinze.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.accounted4.money;

import java.math.RoundingMode;
import java.util.Currency;

/**
 * TODO: yet to be implemented: System defaults overridden by user defaults...
 * 
 * @author Glenn Heinze <glenn@gheinze.com>
 */
class AppConfig {

    public static AppConfig getInstance() {
        return new AppConfig();
    }
    
    public Currency getDefaultCurrency() {
        return Currency.getInstance("CAD");
    }
 
    public RoundingMode getDefaultRoundingMode() {
        return RoundingMode.CEILING;
    }
    
}
