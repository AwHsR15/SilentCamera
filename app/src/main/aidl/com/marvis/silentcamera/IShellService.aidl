package com.marvis.silentcamera;
import android.os.Bundle;

interface IShellService {
    Bundle execute(in String[] argv) = 0;
    String audioGroups() = 1;
    void destroy() = 16777114;
}
