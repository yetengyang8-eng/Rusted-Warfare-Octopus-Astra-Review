package io.rwagent.client;
import java.util.*;
/** Current LR coverage is validated now, rather than restamping an old native eligibility. */
public final class LocalCoverageHarness {
    static int checks;static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        ForceControllerHarness.Fixture f=new ForceControllerHarness.Fixture(4);f.enemy(500,180,200);f.controller.prepareLocalResponse(f.state,f.enemies,f.now);
        Map<String,Object> initial=f.controller.localResponseCoverageView();check(Boolean.TRUE.equals(initial.get("ownCurrent"))&&!((List<?>)initial.get("actorIds")).isEmpty(),"prepare credits only real native-compatible leased responders");
        check(f.proposals.isEmpty(),"preparation does not consume a force token or issue a proposal");
        List<?> actors=(List<?>)initial.get("actorIds");long actor=((Number)actors.get(0)).longValue();String owner=f.registry.unit(actor).owner;
        f.observe(1000);check(Boolean.FALSE.equals(f.controller.localResponseCoverageView().get("ownCurrent")),"later own frame cannot restamp old eligibility");
        f.blocked=true;f.controller.prepareLocalResponse(f.state,f.enemies,f.now);Map<String,Object> unknown=f.controller.localResponseCoverageView();
        check(((List<?>)unknown.get("actorIds")).isEmpty(),"existing LR loses capacity credit when current native approach/compatibility is unavailable");
        check(owner.equals(f.registry.unit(actor).owner),"unknown evidence does not revoke an existing LR owner");
        f.blocked=false;f.controller.prepareLocalResponse(f.state,f.enemies,f.now);check(!((List<?>)f.controller.localResponseCoverageView().get("actorIds")).isEmpty(),"new valid current eligibility restores real coverage");
        f.enemyRows.clear();f.controller.prepareLocalResponse(f.state,f.enemies,f.now);check(((List<?>)f.controller.localResponseCoverageView().get("actorIds")).isEmpty(),"visibility disappearance immediately clears coverage without kill inference");
        check(GeneralRegistry.FREE_OWNER.equals(f.registry.unit(actor).owner)&&f.controller.localClearStagingHold(actor,f.now),"clear returns FREE with bounded non-attacking staging hold");
        System.out.println("LocalCoverageHarness PASS checks="+checks);
    }
}
