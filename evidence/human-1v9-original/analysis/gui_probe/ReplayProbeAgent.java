package analysis;
import java.lang.instrument.*;
import java.security.ProtectionDomain;
import java.io.ByteArrayInputStream;
import javassist.*;
public class ReplayProbeAgent {
  public static void premain(String opts, Instrumentation inst) {
    System.err.println("[RWPROBE] premain");
    inst.addTransformer(new ClassFileTransformer(){
      @Override public byte[] transform(ClassLoader loader,String name,Class<?> cls,ProtectionDomain pd,byte[] original){
        if(!"com/corrodinggames/rts/game/i".equals(name)) return null;
        try {
          ClassPool cp=new ClassPool(true);
          if(loader!=null)cp.appendClassPath(new LoaderClassPath(loader));
          CtClass target=cp.makeClass(new ByteArrayInputStream(original));
          CtMethod m=target.getDeclaredMethod("b",new CtClass[]{CtClass.floatType,CtClass.intType});
          m.insertAfter("{ analysis.ReplayProbeSampler.onFrame(this); }");
          byte[] modified=target.toBytecode();target.detach();
          System.err.println("[RWPROBE] engine hook OK");
          return modified;
        }catch(Throwable t){System.err.println("[RWPROBE] hook FAILED "+t);t.printStackTrace();return null;}
      }
    });
  }
}
