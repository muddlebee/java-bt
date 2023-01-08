/*
 * Copyright (c) 2016—2021 Andrei Tomashpolskiy and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package yourip;

import bt.Bt;
import bt.data.Storage;
import bt.data.file.FileSystemStorage;
import bt.dht.DHTConfig;
import bt.dht.DHTModule;
import bt.net.InetPeer;
import bt.net.Peer;
import bt.runtime.BtClient;
import bt.runtime.Config;
import com.google.inject.Module;
import yourip.mock.MockModule;
import yourip.mock.MockStorage;
import yourip.mock.MockTorrent;

import java.net.InetAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Main {

    private static final int[] ports = new int[]{6891, 6892};
    private static final Set<Peer> peers = new HashSet<Peer>() {{
        for (int port : ports) {
            add(InetPeer.build(InetAddress.getLoopbackAddress(), port));
        }
    }};

    public static Set<Peer> peers() {
        return Collections.unmodifiableSet(peers);
    }

    public static void main(String[] args) throws InterruptedException {
        // enable multithreaded verification of torrent data
        Config config = new Config() {
            @Override
            public int getNumOfHashingThreads() {
                return Runtime.getRuntime().availableProcessors() * 2;
            }
        };

// enable bootstrapping from public routers
        Module dhtModule = new DHTModule(new DHTConfig() {
            @Override
            public boolean shouldUseRouterBootstrap() {
                return true;
            }
        });

// get download directory
        Path targetDirectory = Paths.get(System.getProperty("user.home"), "Downloads");

// create file system based backend for torrent data
        Storage storage = new FileSystemStorage(targetDirectory);

// create client with a private runtime
        BtClient client = Bt.client()
                .config(config)
                .storage(storage)
                .magnet("magnet:?xt=urn:btih:289eeab1f57a4b4777113a4f86eb5a4f33c7b2ee&dn=Puss.in.Boots.The.Last.Wish.2022.720p.WEBRip.800MB.x264-GalaxyRG&tr=udp%3A%2F%2Fopen.stealth.si%3A80%2Fannounce&tr=udp%3A%2F%2Fexodus.desync.com%3A6969%2Fannounce&tr=udp%3A%2F%2Ftracker.cyberia.is%3A6969%2Fannounce&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337%2Fannounce&tr=udp%3A%2F%2Ftracker.torrent.eu.org%3A451%2Fannounce&tr=udp%3A%2F%2Fexplodie.org%3A6969%2Fannounce&tr=udp%3A%2F%2Ftracker.birkenwald.de%3A6969%2Fannounce&tr=udp%3A%2F%2Ftracker.moeking.me%3A6969%2Fannounce&tr=udp%3A%2F%2Fipv4.tracker.harry.lu%3A80%2Fannounce&tr=udp%3A%2F%2F9.rarbg.me%3A2970%2Fannounce")
                .autoLoadModules()
           //     .module(dhtModule)
                .stopWhenDownloaded()
                .build();

// launch
        client.startAsync().join();
    }

    private static BtClient buildClient(int port) {
        Config config = new Config() {
            @Override
            public InetAddress getAcceptorAddress() {
                return InetAddress.getLoopbackAddress();
            }

            @Override
            public int getAcceptorPort() {
                return port;
            }

            @Override
            public Duration getPeerDiscoveryInterval() {
                return Duration.ofSeconds(1);
            }

            @Override
            public Duration getTrackerQueryInterval() {
                return Duration.ofSeconds(1);
            }
        };

        return Bt.client()
                .config(config)
                .module(YourIPModule.class)
                .module(MockModule.class)
                .storage(new MockStorage())
                .torrent(() -> new MockTorrent())
                .build();
    }

}
