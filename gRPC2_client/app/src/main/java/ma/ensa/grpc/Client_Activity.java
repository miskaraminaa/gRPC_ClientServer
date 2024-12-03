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

        // Initialiser les éléments de l'interface utilisateur
        etMontant = findViewById(R.id.et_montant);
        etDeviseSource = findViewById(R.id.et_devise_source);
        etDeviseCible = findViewById(R.id.et_devise_cible);
        tvResultat = findViewById(R.id.tv_resultat);
        btnConvertir = findViewById(R.id.btn_convertir);

        // Configurer le canal gRPC
        configurerCanalGrpc();

        // Gestionnaire d'événements pour le bouton
        btnConvertir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                convertirDevise();
            }
        });
    }

    private void configurerCanalGrpc() {
        try {
            // Utiliser 10.0.2.2 pour accéder à la machine hôte depuis l'émulateur Android
            canal = ManagedChannelBuilder.forAddress("10.0.2.2", 5555)
                    .usePlaintext() // HTTP/2 sans TLS
                    .keepAliveTimeout(30, TimeUnit.SECONDS)
                    .keepAliveTime(30, TimeUnit.SECONDS)
                    .build();

            // Créer un stub bloquant pour les appels synchrones
            stub = BankServiceGrpc.newBlockingStub(canal);

        } catch (Exception e) {
            Log.e("Erreur gRPC", "Erreur lors de la configuration du canal gRPC : " + e.getMessage(), e);
            Toast.makeText(this, "Échec de la configuration du canal gRPC", Toast.LENGTH_SHORT).show();
        }
    }

    private void convertirDevise() {
        // Récupérer les valeurs saisies
        String montantStr = etMontant.getText().toString();
        String deviseSource = etDeviseSource.getText().toString();
        String deviseCible = etDeviseCible.getText().toString();

        if (montantStr.isEmpty() || deviseSource.isEmpty() || deviseCible.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        double montant;
        try {
            montant = Double.parseDouble(montantStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Montant invalide", Toast.LENGTH_SHORT).show();
            return;
        }

        // Créer une requête gRPC
        Bank.ConvertCurrencyRequest requete = Bank.ConvertCurrencyRequest.newBuilder()
                .setAmount(montant)
                .setCurrencyFrom(deviseSource)
                .setCurrencyTo(deviseCible)
                .build();

        new Thread(() -> {
            try {
                Bank.ConvertCurrencyResponse reponse = stub.convert(requete);

                runOnUiThread(() -> tvResultat.setText("Montant converti : " + reponse.getResult()));

            } catch (StatusRuntimeException e) {
                Log.e("Erreur gRPC", "Erreur lors de l'appel gRPC : " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(this, "Échec de la conversion : " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                Log.e("Erreur gRPC", "Erreur inattendue : " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(this, "Une erreur s'est produite", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (canal != null && !canal.isShutdown()) {
            canal.shutdown();
        }
    }
}
