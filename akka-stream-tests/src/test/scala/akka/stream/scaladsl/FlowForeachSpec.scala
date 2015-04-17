/**
 * Copyright (C) 2014 Typesafe Inc. <http://www.typesafe.com>
 */
package akka.stream.scaladsl

import scala.util.control.NoStackTrace
import akka.stream.ActorFlowMaterializer
import akka.stream.testkit.{ AkkaSpec, StreamTestKit }
import scala.concurrent.Await
import akka.stream.testkit.StreamTestKit.checkThatAllStagesAreStopped

class FlowForeachSpec extends AkkaSpec {

  implicit val mat = ActorFlowMaterializer()
  import system.dispatcher

  "A Foreach" must {

    "call the procedure for each element" in checkThatAllStagesAreStopped {
      Source(1 to 3).runForeach(testActor ! _) onSuccess {
        case _ ⇒ testActor ! "done"
      }
      expectMsg(1)
      expectMsg(2)
      expectMsg(3)
      expectMsg("done")
    }

    "complete the future for an empty stream" in checkThatAllStagesAreStopped {
      Source.empty[String].runForeach(testActor ! _) onSuccess {
        case _ ⇒ testActor ! "done"
      }
      expectMsg("done")
    }

    "yield the first error" in checkThatAllStagesAreStopped {
      val p = StreamTestKit.PublisherProbe[Int]()
      Source(p).runForeach(testActor ! _) onFailure {
        case ex ⇒ testActor ! ex
      }
      val proc = p.expectSubscription
      proc.expectRequest()
      val ex = new RuntimeException("ex") with NoStackTrace
      proc.sendError(ex)
      expectMsg(ex)
    }

    "complete future with failure when function throws" in checkThatAllStagesAreStopped {
      val error = new Exception with NoStackTrace
      val future = Source.single(1).runForeach(_ ⇒ throw error)
      the[Exception] thrownBy Await.result(future, remaining) should be(error)
    }

  }

}
