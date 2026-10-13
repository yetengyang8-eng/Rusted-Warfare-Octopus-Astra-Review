package analysis;
import javassist.*;
public final class PatchGameEngine {
 public static void main(String[] args) throws Exception {
   ClassPool pool=new ClassPool(true);
   pool.appendClassPath(args[0]);
   pool.appendClassPath(args[1]);
   CtClass cls=pool.get("com.corrodinggames.rts.game.i");
   CtMethod tick=cls.getDeclaredMethod("b", new CtClass[]{CtClass.floatType,CtClass.intType});
   tick.insertAfter("{ analysis.ReplayProbeSampler.onFrame(this); }");
   byte[] bytes=cls.toBytecode();
   java.nio.file.Files.write(java.nio.file.Path.of(args[2]),bytes);
   System.out.println("PATCHED_ENGINE_BYTES="+bytes.length);
   cls.detach();
 }
}
