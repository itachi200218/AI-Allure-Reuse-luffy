//package AI;
//
///**
// * Returns the currently running TestNG class name as the project name
// */
//public class ProjectContext {
//
//    public static String getCurrentProjectName() {
//        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
//        for (StackTraceElement elem : stack) {
//            String cls = elem.getClassName();
//            if (cls.startsWith("org.")) { // adjust package prefix of your test classes
//                return cls.substring(cls.lastIndexOf(".") + 1); // simple class name
//            }
//        }
//        return "DefaultProject"; // fallback
//    }
//}
