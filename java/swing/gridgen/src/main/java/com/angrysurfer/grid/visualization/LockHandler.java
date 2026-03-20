package com.angrysurfer.grid.visualization;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LockHandler {

    private boolean locked = false;

    public LockHandler() {
    }

    public void lockDisplay() {
        locked = true;
    }

    public void unlockDisplay() {
        locked = false;
    }
}