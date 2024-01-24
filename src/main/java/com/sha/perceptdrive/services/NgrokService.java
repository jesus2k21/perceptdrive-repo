package com.sha.perceptdrive.services;

import com.github.alexdlaird.ngrok.NgrokClient;
import com.github.alexdlaird.ngrok.protocol.CreateTunnel;
import com.github.alexdlaird.ngrok.protocol.Tunnel;
import com.sha.perceptdrive.config.SpringProfiles;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile( {SpringProfiles.LOCAL} )
public class NgrokService {

    public NgrokService() {
        final NgrokClient ngrokClient = new NgrokClient.Builder().build();
        final CreateTunnel tunnelBuilder = new CreateTunnel.Builder()
                .withAddr(8080)
                .build();
        final Tunnel tunnel = ngrokClient.connect(tunnelBuilder);
    }
}
