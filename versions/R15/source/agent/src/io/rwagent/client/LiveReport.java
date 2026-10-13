package io.rwagent.client;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Display-only latest facts. The archive remains the complete causal evidence. */
final class LiveReport {
    private final Path target;
    private final String journal;
    private final Map<String,String> rows=new LinkedHashMap<String,String>();
    private boolean warned;
    LiveReport(File directory,String journal){this.target=new File(directory,"battle-latest.json").toPath();this.journal=journal;}
    void accept(String kind,String data){
        if(!Arrays.asList("report_provenance","g5_runtime_state","summary","match_outcome").contains(kind))return;
        rows.put(kind,"{\"event\":"+BattleClient.json(kind)+",\"data\":"+data+"}");
        String content="{\"schemaVersion\":1,\"journal\":"+BattleClient.json(journal)+",\"rows\":["+String.join(",",rows.values())+"]}";
        Path pending=target.resolveSibling(target.getFileName()+".tmp");
        try{
            Files.write(pending,content.getBytes(StandardCharsets.UTF_8));
            try{Files.move(pending,target,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException e){Files.move(pending,target,StandardCopyOption.REPLACE_EXISTING);}
        }catch(IOException e){if(!warned){warned=true;System.err.println("Live display snapshot unavailable; journal/controller continue: "+e.getMessage());}}
    }
}
