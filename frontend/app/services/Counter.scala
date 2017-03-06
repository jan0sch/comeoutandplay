/*
 * Copyright (C) 2017  Jens Grassel & André Schütz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package services

import java.util.concurrent.atomic.AtomicInteger
import javax.inject._

/**
  * This trait demonstrates how to create a component that is injected
  * into a controller. The trait represents a counter that returns a
  * incremented number each time it is called.
  */
trait Counter {
  def nextCount(): Int
}

/**
  * This class is a concrete implementation of the [[Counter]] trait.
  * It is configured for Guice dependency injection in the [[Module]]
  * class.
  *
  * This class has a `Singleton` annotation because we need to make
  * sure we only use one counter per application. Without this
  * annotation we would get a new instance every time a [[Counter]] is
  * injected.
  */
@Singleton
class AtomicCounter extends Counter {
  private val atomicCounter     = new AtomicInteger()
  override def nextCount(): Int = atomicCounter.getAndIncrement()
}
