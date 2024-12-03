package ma.ensa.grpc;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.example.stub.Bank;
import org.example.stub.BankServiceGrpc;

import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

public class Client_Activity extends AppCompatActivity {

    private EditText etMontant, etDeviseSource, etDeviseCible;
    private TextView tvResultat;
    private Button btnConvertir;

    private ManagedChannel canal;
    private BankServiceGrpc.BankServiceBlockingStub stub;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        // Initialisation des éléments UI
        initialiserUI();

        // Configuration du canal gRPC
        configurerCanalGrpc();

        // Gestionnaire d'événements pour le bouton de conversion
        btnConvertir.setOnClickListener(v -> convertirDevise());
    }

    private void initialiserUI() {
        etMontant = findViewById(R.id.et_montant);
        etDeviseSource = findViewById(R.id.et_devise_source);
        etDeviseCible = findViewById(R.id.et_devise_cible);
        tvResultat = findViewById(R.id.tv_resultat);
        btnConvertir = findViewById(R.id.btn_convertir);
    }

    private void configurerCanalGrpc() {
        try {
            // Configuration du canal gRPC avec paramètres optimisés
            canal = ManagedChannelBuilder.forAddress("10.0.2.2", 5555)
                    .usePlaintext() // Utiliser HTTP/2 sans cryptage (dev/test uniquement)
                    .keepAliveTimeout(30, TimeUnit.SECONDS)
                    .keepAliveTime(30, TimeUnit.SECONDS)
                    .build();

            // Créer un stub bloquant pour les appels synchrones
            stub = BankServiceGrpc.newBlockingStub(canal);

        } catch (Exception e) {
            Log.e("Erreur gRPC", "Erreur lors de la configuration du canal : " + e.getMessage(), e);
            afficherToast("Impossible de configurer le canal gRPC.");
        }
    }

    private void convertirDevise() {
        // Validation des données utilisateur
        String montantStr = etMontant.getText().toString();
        String deviseSource = etDeviseSource.getText().toString().toUpperCase();
        String deviseCible = etDeviseCible.getText().toString().toUpperCase();

        if (!validerChamps(montantStr, deviseSource, deviseCible)) {
            return;
        }

        double montant = Double.parseDouble(montantStr);

        // Construction de la requête gRPC
        Bank.ConvertCurrencyRequest requete = Bank.ConvertCurrencyRequest.newBuilder()
                .setAmount(montant)
                .setCurrencyFrom(deviseSource)
                .setCurrencyTo(deviseCible)
                .build();

        // Exécution de l'appel gRPC dans un thread séparé
        new Thread(() -> {
            try {
                // Appel gRPC
                Bank.ConvertCurrencyResponse reponse = stub.convert(requete);

                // Mise à jour de l'UI avec le résultat
                runOnUiThread(() -> tvResultat.setText("Montant converti : " + reponse.getResult()));

            } catch (StatusRuntimeException e) {
                Log.e("Erreur gRPC", "Erreur lors de l'appel : " + e.getMessage(), e);
                afficherToast("Conversion échouée. Veuillez réessayer.");
            } catch (Exception e) {
                Log.e("Erreur gRPC", "Erreur inattendue : " + e.getMessage(), e);
                afficherToast("Une erreur inattendue s'est produite.");
            }
        }).start();
    }

    private boolean validerChamps(String montantStr, String deviseSource, String deviseCible) {
        if (montantStr.isEmpty() || deviseSource.isEmpty() || deviseCible.isEmpty()) {
            afficherToast("Veuillez remplir tous les champs.");
            return false;
        }

        try {
            Double.parseDouble(montantStr);
        } catch (NumberFormatException e) {
            afficherToast("Montant invalide. Entrez un nombre valide.");
            return false;
        }

        if (deviseSource.length() != 3 || deviseCible.length() != 3) {
            afficherToast("Les devises doivent être des codes à 3 lettres (ex : USD, EUR).");
            return false;
        }

        return true;
    }

    private void afficherToast(String message) {
        runOnUiThread(() -> Toast.makeText(Client_Activity.this, message, Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Libérer les ressources gRPC
        if (canal != null && !canal.isShutdown()) {
            canal.shutdown();
        }
    }
}
