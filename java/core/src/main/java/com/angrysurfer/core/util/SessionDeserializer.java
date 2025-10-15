package com.angrysurfer.core.util;

import java.io.IOException;

import com.angrysurfer.core.model.Session;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class SessionDeserializer extends StdDeserializer<Session> {
    
    public SessionDeserializer() {
        this(null);
    }
    
    public SessionDeserializer(Class<?> vc) {
        super(vc);
    }
    
    @Override
    public Session deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        
    // Create a new session that will properly initialize all transient fields
    Session session = new Session();
    session.initialize();
        
        // Read ID and name properties
        if (node.has("id")) {
            session.setId(node.get("id").asLong());
        }
        if (node.has("name")) {
            session.setName(node.get("name").asText());
        }
        
        // Read tempo and timing properties
        if (node.has("tempoInBPM")) {
            session.setTempoInBPM(node.get("tempoInBPM").floatValue());
        }
        if (node.has("ticksPerBeat")) {
            session.setTicksPerBeat(node.get("ticksPerBeat").intValue());
        }
        if (node.has("beatsPerBar")) {
            session.setBeatsPerBar(node.get("beatsPerBar").intValue());
        }
        if (node.has("bars")) {
            session.setBars(node.get("bars").intValue());
        }
        if (node.has("parts")) {
            session.setParts(node.get("parts").intValue());
        }
        if (node.has("partLength")) {
            session.setPartLength(node.get("partLength").intValue());
        }
        
        // Read scale and root note
        if (node.has("scale")) {
            session.setScale(node.get("scale").textValue());
        }
        if (node.has("rootNote")) {
            session.setRootNote(node.get("rootNote").textValue());
        }
        
        // Read noteOffset and other musical parameters
        if (node.has("noteOffset")) {
            session.setNoteOffset(node.get("noteOffset").intValue());
        }
        if (node.has("swing")) {
            session.setSwing(node.get("swing").intValue());
        }
        
        // Read other session info
        if (node.has("notes")) {
            session.setNotes(node.get("notes").textValue());
        }
        if (node.has("loopCount")) {
            session.setLoopCount(node.get("loopCount").intValue());
        }
        
        // Read active player IDs
        if (node.has("activePlayerIds") && node.get("activePlayerIds").isArray()) {
            JsonNode playerIdsNode = node.get("activePlayerIds");
            for (JsonNode idNode : playerIdsNode) {
                session.getActivePlayerIds().add(idNode.longValue());
            }
        }
        
        return session;
    }
}