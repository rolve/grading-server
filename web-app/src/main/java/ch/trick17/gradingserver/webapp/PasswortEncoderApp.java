package ch.trick17.gradingserver.webapp;

import org.springframework.security.crypto.factory.PasswordEncoderFactories;

import java.util.Scanner;

public class PasswortEncoderApp {

    public static void main(String[] args) {
        var encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        System.out.print("Passwort? ");
        var password = new Scanner(System.in).nextLine();
        System.out.println(encoder.encode(password));
    }
}
