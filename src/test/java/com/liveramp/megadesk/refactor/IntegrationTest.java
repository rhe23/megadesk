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

package com.liveramp.megadesk.refactor;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.curator.test.TestingServer;
import org.apache.log4j.Logger;

import com.liveramp.megadesk.refactor.attempt.Outcome;
import com.liveramp.megadesk.refactor.gear.Gear;
import com.liveramp.megadesk.refactor.lib.curator.CuratorDriver;
import com.liveramp.megadesk.refactor.lib.curator.CuratorGear;
import com.liveramp.megadesk.refactor.lib.curator.CuratorNode;
import com.liveramp.megadesk.refactor.node.Node;
import com.liveramp.megadesk.refactor.worker.NaiveWorker;
import com.liveramp.megadesk.refactor.worker.Worker;
import com.liveramp.megadesk.test.BaseTestCase;

public class IntegrationTest extends BaseTestCase {

  private static final Logger LOG = Logger.getLogger(IntegrationTest.class);

  public void testMain() throws Exception {

    TestingServer testingServer = new TestingServer(12000);
    CuratorDriver driver = new CuratorDriver(testingServer.getConnectString());

    final AtomicInteger resource1 = new AtomicInteger();
    final AtomicInteger resource2 = new AtomicInteger();
    final AtomicBoolean resource3 = new AtomicBoolean();

    Node node1 = new CuratorNode(driver, "/1");
    Node node2 = new CuratorNode(driver, "/2");
    Node node3 = new CuratorNode(driver, "/3");

    Gear gearA = new CuratorGear(driver, "/a") {

      @Override
      public boolean isRunnable() {
        return resource1.get() < 5;
      }

      @Override
      public Outcome run() throws Exception {
        resource1.incrementAndGet();
        LOG.info("gearA: resource1 is now " + resource1.get());
        return Outcome.SUCCESS;
      }
    }.writes(node1);

    Gear gearB = new CuratorGear(driver, "/b") {

      @Override
      public boolean isRunnable() {
        return resource1.get() == 5;
      }

      @Override
      public Outcome run() throws Exception {
        resource1.set(0);
        resource2.incrementAndGet();
        LOG.info("gearB: resource1 is now " + resource1.get());
        LOG.info("gearB: resource2 is now " + resource2.get());
        return Outcome.SUCCESS;
      }
    }.reads(node1).writes(node1, node2);

    Gear gearC = new CuratorGear(driver, "/c") {

      @Override
      public boolean isRunnable() {
        return !resource3.get() && resource2.get() == 2;
      }

      @Override
      public Outcome run() throws Exception {
        resource3.set(true);
        LOG.info("gearC: resource3 is now " + resource3.get());
        return Outcome.SUCCESS;
      }
    }.reads(node2, node3).writes(node3);

    // Run

    Worker worker = new NaiveWorker();

    worker.run(gearA);
    worker.run(gearB);
    worker.run(gearC);

    worker.join();

  }
}