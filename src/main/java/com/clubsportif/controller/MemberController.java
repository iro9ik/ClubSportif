package com.clubsportif.controller;

import com.clubsportif.model.Member;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import java.util.ArrayList;
import java.util.List;

public class MemberController {

    @FXML private Label salaireMoyen;

    private List<Member> members = new ArrayList<>();

    @FXML
    public void afficherSalaireMoyen() {
        double moyenne = members.stream()
                .mapToDouble(Member::getSalaire)
                .average()
                .orElse(0);

        salaireMoyen.setText("Salaire Moyen : " + moyenne + " DH");
    }
}
