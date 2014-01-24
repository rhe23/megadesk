/**
 *  Copyright 2014 LiveRamp
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.liveramp.megadesk.core.transaction;

import com.liveramp.megadesk.base.transaction.ExecutionResult;
import com.liveramp.megadesk.core.state.Driver;

public interface Executor {

  <V> V execute(Transaction<V> transaction) throws Exception;

  <V> V execute(UnboundTransaction<V> transaction, Driver... arguments) throws Exception;

  <V> V execute(Transaction<V> transaction, Driver<V> result) throws Exception;

  <V> ExecutionResult<V> tryExecute(Transaction<V> transaction) throws Exception;

  <V> ExecutionResult<V> tryExecute(Transaction<V> transaction, Driver<V> result) throws Exception;
}