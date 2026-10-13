package io.rwagent.client;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
/** Real atomic file replacement; display failure has no effect on execution. */
public final class R14LiveReportHarness {
    public static void main(String[]args)throws Exception{
        Path dir=Files.createTempDirectory("r14-live-");LiveReport feed=new LiveReport(dir.toFile(),"battle-example.jsonl.gz");
        feed.accept("g5_runtime_state","{\"gameTimeMs\":100,\"sourceFrame\":4,\"credits\":90}");
        feed.accept("g3_execution","{\"ignored\":true}");
        feed.accept("g5_runtime_state","{\"gameTimeMs\":200,\"sourceFrame\":8,\"credits\":70}");
        feed.accept("summary","{\"outcome\":\"PARTIAL\",\"matchOutcome\":\"ONGOING\"}");
        String text=new String(Files.readAllBytes(dir.resolve("battle-latest.json")),StandardCharsets.UTF_8);
        if(!text.contains("\"gameTimeMs\":200")||text.contains("\"gameTimeMs\":100")||text.contains("ignored")||!text.contains("ONGOING"))throw new AssertionError(text);
        if(Files.exists(dir.resolve("battle-latest.json.tmp")))throw new AssertionError("incomplete snapshot");
        new LiveReport(dir.resolve("absent").toFile(),"failure").accept("summary","{}");
        Files.delete(dir.resolve("battle-latest.json"));Files.delete(dir);
        System.out.println("R14_LIVE_REPORT_OK checks=6 realFile=true");
    }
}
