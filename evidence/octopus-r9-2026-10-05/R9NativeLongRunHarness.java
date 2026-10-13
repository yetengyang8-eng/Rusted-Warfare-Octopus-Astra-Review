import java.nio.file.*;

import java.nio.charset.StandardCharsets;
/** Controlled native queue delay, not a natural 180-second production claim. */
public class R9NativeLongRunHarness {
 public static void main(String[] args)throws Exception {
  G5NativeOperationsHarness.out=Paths.get(args[0]).toAbsolutePath();Files.createDirectories(G5NativeOperationsHarness.out);G5NativeOperationsHarness.bootstrap();
  G3NativeAcceptanceHarness.base(8,2,20000);
  G3NativeAcceptanceHarness.Client c=new G3NativeAcceptanceHarness.Client("r9-delayed-native-queue",256,false);
  c.advance(1000,false);c.produce();
  G3NativeAcceptanceHarness.require(G3NativeAcceptanceHarness.engine.cf.b.size()==2,"two actual producers accepted independently");
  G3NativeAcceptanceHarness.require(c.scheduler.unsettledCreditCount()==2,"accepted invisible effects retain two actual native-price obligations");
  double held=c.scheduler.unsettledCreditTotal();
  c.advance(181000,false);
  G3NativeAcceptanceHarness.require(c.scheduler.unsettledCreditTotal()==held,"181-second artificial clock cannot refund unknown native payment");
  G3NativeAcceptanceHarness.require(c.battle.orderStrategy("rule-main","/command/move?unitId=2&x="+(((Number)c.actor(2).get("x")).doubleValue()+20)+"&y="+c.actor(2).get("y"))!=null,"unrelated own Builder command remains possible after production witness timeout");
  G3NativeAcceptanceHarness.process();c.advance(1000,false);
  G3NativeAcceptanceHarness.require(c.scheduler.unsettledCreditCount()==0,"later real native queues settle actual payment obligations");
  G3NativeAcceptanceHarness.require(((Number)c.actor(50).get("productionQueue")).intValue()>0,"first real native queue still busy, never fabricated ready");
  G3NativeAcceptanceHarness.require(((Number)c.actor(51).get("productionQueue")).intValue()>0,"second real native queue still busy, unrelated queues preserved");
  c.close();G3NativeAcceptanceHarness.fixture.close();
  Files.write(G5NativeOperationsHarness.out.resolve("summary.json"),G3NativeAcceptanceHarness.json(G3NativeAcceptanceHarness.map("status","PASS","checks",G3NativeAcceptanceHarness.checks,"simulationTicks",0,"artificialClockJumpGameMs",181000,"naturalTimeoutObserved",false,"desktopTouched",false,"evidence","REAL_NATIVE_HTTP_DELAYED_COMMAND_QUEUE_FIXTURE")).getBytes(StandardCharsets.UTF_8));
  System.out.println("R9_NATIVE_LONG_RUN_OK checks="+G3NativeAcceptanceHarness.checks+" fixtureOnly=true simulationTicks=0");System.exit(0);
 }
}
