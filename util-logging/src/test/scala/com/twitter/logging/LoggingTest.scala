/*
 * Copyright 2010 Twitter, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.twitter.logging

import com.twitter.app.App
import org.scalatest.{BeforeAndAfterEach, BeforeAndAfterAll, FunSuite}

class LoggingTest extends FunSuite with BeforeAndAfterEach with BeforeAndAfterAll {
  // ensure this is initialized--we need to do something with it or scala
  // complains about purity
  require(ScribeHandler.log != null)

  object TestLoggingApp extends App with Logging {
    override def handlers = ScribeHandler() :: super.handlers
  }

  object TestLoggingWithConfigureApp extends App with Logging {
    override def handlers = ScribeHandler() :: super.handlers
    override def configureLoggerFactories(): Unit = {}
  }

  test("TestLoggingApp should have one factory with two log handlers") {
    TestLoggingApp.main(Array.empty)
    assert(TestLoggingApp.loggerFactories.size == 1)
    assert(TestLoggingApp.loggerFactories.head.handlers.size == 2)
    // Logger is configured with two handlers, and has one logger
    assert(Logger.iterator.size == 1)
  }

  test("TestLoggingWithConfigureApp should set up a Logger with no Loggers") {
    TestLoggingWithConfigureApp.main(Array.empty)
    assert(TestLoggingApp.loggerFactories.size == 1)
    assert(TestLoggingWithConfigureApp.loggerFactories.head.handlers.size == 2)
    // Logger is not configured with any handler/logger
    assert(Logger.iterator.isEmpty)
  }

  override protected def afterAll(): Unit = {
    Logger.reset()
  }

  override protected def beforeEach(): Unit = {
    Logger.reset()
  }
}
