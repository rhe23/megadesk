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

package com.liveramp.megadesk.transaction;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import com.google.common.collect.Sets;

import com.liveramp.megadesk.state.Driver;
import com.liveramp.megadesk.state.Value;

public class BaseTransaction implements Transaction {

  public enum State {
    STANDBY,
    RUNNING,
    COMMITTED,
    ABORTED
  }

  private TransactionDependency dependency;
  private TransactionData data;
  private State state = State.STANDBY;
  private final Set<Lock> locked;

  public BaseTransaction() {
    locked = Sets.newHashSet();
  }

  @Override
  public TransactionData begin(TransactionDependency dependency) {
    ensureState(State.STANDBY);
    lock(dependency);
    return setUp(dependency);
  }

  @Override
  public TransactionData tryBegin(TransactionDependency dependency) {
    ensureState(State.STANDBY);
    boolean result = tryLock(dependency);
    if (result) {
      return setUp(dependency);
    } else {
      return null;
    }
  }

  private TransactionData setUp(TransactionDependency dependency) {
    this.state = State.RUNNING;
    this.dependency = dependency;
    this.data = new BaseTransactionData(dependency);
    return this.data;
  }

  @Override
  public void commit() {
    ensureState(State.RUNNING);
    // Write updates
    for (Driver driver : dependency.writes()) {
      Value value = data.binding(driver.reference()).read();
      driver.persistence().write(value);
    }
    // Release locks
    unlock(locked);
    state = State.COMMITTED;
  }

  @Override
  public void abort() {
    ensureState(State.RUNNING);
    unlock(locked);
    state = State.ABORTED;
  }

  private boolean tryLock(TransactionDependency dependency) {
    for (Driver read : dependency.reads()) {
      // TODO is this necessary?
      // Skip to avoid deadlocks
      if (dependency.writes().contains(read)) {
        continue;
      }
      if (!tryLockAndRemember(read.lock().readLock(), locked)) {
        unlock(locked);
        return false;
      }
    }
    for (Driver write : dependency.writes()) {
      if (!write.lock().writeLock().tryLock()) {
        unlock(locked);
        return false;
      }
    }
    return true;
  }

  private void lock(TransactionDependency dependency) {
    for (Driver read : dependency.reads()) {
      // TODO is this necessary?
      // Skip to avoid deadlocks
      if (dependency.writes().contains(read)) {
        continue;
      }
      lockAndRemember(read.lock().readLock(), locked);
    }
    for (Driver write : dependency.writes()) {
      lockAndRemember(write.lock().writeLock(), locked);
    }
  }

  private static boolean tryLockAndRemember(Lock lock, Set<Lock> locked) {
    boolean result = lock.tryLock();
    if (result) {
      locked.add(lock);
    }
    return result;
  }

  private static void lockAndRemember(Lock lock, Set<Lock> locked) {
    lock.lock();
    locked.add(lock);
  }

  private static void unlock(Set<Lock> locked) {
    Iterator<Lock> lockIterator = locked.iterator();
    while (lockIterator.hasNext()) {
      lockIterator.next().unlock();
      lockIterator.remove();
    }
  }

  private void ensureState(State state) {
    if (this.state != state) {
      throw new IllegalStateException("Transaction state should be " + state + " but is " + this.state);
    }
  }
}