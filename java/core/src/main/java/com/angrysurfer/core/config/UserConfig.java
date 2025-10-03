package com.angrysurfer.core.config;

import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Note;
import com.angrysurfer.core.model.Strike;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserConfig {
    private Integer id;
    private List<InstrumentWrapper> defaultInstruments = new ArrayList<>();
    private List<Strike> defaultStrikes = new ArrayList<>();
    // private List<Sample> defaultSamplers = new ArrayList<>();
    private List<Note> defaultNotes = new ArrayList<>();
    private Boolean hasDefaults = false;
    private int configVersion = 1;
    private Date lastUpdated;
    private String name;
    private boolean metronomeAudible = true;
}
