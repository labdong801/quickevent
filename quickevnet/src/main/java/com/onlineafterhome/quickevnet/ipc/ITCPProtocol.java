package com.onlineafterhome.quickevnet.ipc;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public interface ITCPProtocol {
    //accept I/O形式
    void handleAccept(SelectionKey key) throws IOException;
    //read I/O形式
    void handleRead(SelectionKey key) throws IOException;
    //write I/O形式
    void handleWrite(SelectionKey key) throws IOException;
}
