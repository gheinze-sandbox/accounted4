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

import java.util.Currency;

/**
 * Thrown if an operation is attempted on Moneys with differing currencies.
 * 
 * @author Glenn Heinze <glenn@gheinze.com>
 */
public class CurrencyMismatchRuntimeException extends RuntimeException {
    
    public CurrencyMismatchRuntimeException(Currency base, Currency operand) {
        super(String.format("Currency mismatch: [%s, %s]", base.toString(), operand.toString()));
    }
    
}
