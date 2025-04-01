import android.os.Bundle
import android.content.Intent
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private lateinit var registerEmailEditText: EditText
    private lateinit var registerNameEditText: EditText
    private lateinit var registerSectorEditText: EditText
    private lateinit var registerMatriculaEditText: EditText
    private lateinit var registerPasswordEditText: EditText
    private lateinit var registerUserButton: Button
    private lateinit var passwordToggleImageView: ImageView

    private var isPasswordVisible: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        registerEmailEditText = findViewById(R.id.registerEmailEditText)
        registerNameEditText = findViewById(R.id.registerNameEditText)
        registerSectorEditText = findViewById(R.id.registerSectorEditText)
        registerMatriculaEditText = findViewById(R.id.registerMatriculaEditText)
        registerPasswordEditText = findViewById(R.id.registerPasswordEditText)
        registerUserButton = findViewById(R.id.registerUserButton)
        passwordToggleImageView = findViewById(R.id.passwordToggleImageView)

        passwordToggleImageView.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                registerPasswordEditText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                passwordToggleImageView.setImageResource(R.drawable.ic_visibility)
            } else {
                registerPasswordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                passwordToggleImageView.setImageResource(R.drawable.ic_visibility_off)
            }
            registerPasswordEditText.setSelection(registerPasswordEditText.text.length)
        }

        registerUserButton.setOnClickListener {
            val email = registerEmailEditText.text.toString()
            val name = registerNameEditText.text.toString()
            val sector = registerSectorEditText.text.toString()
            val matricula = registerMatriculaEditText.text.toString()
            val password = registerPasswordEditText.text.toString()

            if (email.isEmpty() || name.isEmpty() || sector.isEmpty() || matricula.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, preencha todos os campos.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Salvar dados temporários na seção de registro
            val tempUser = User("", name, email, sector, matricula)
            val tempKey = database.child("registration").push().key ?: ""
            database.child("registration").child(tempKey).setValue(tempUser)
                .addOnCompleteListener { tempTask ->
                    if (tempTask.isSuccessful) {
                        // Criar o usuário no Firebase Auth
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(this) { task ->
                                if (task.isSuccessful) {
                                    val user = auth.currentUser
                                    val profileUpdates = UserProfileChangeRequest.Builder()
                                        .setDisplayName(name)
                                        .build()
                                    user?.updateProfile(profileUpdates)?.addOnCompleteListener { updateTask ->
                                        if (updateTask.isSuccessful) {
                                            saveUserToDatabase(user.uid, name, email, sector, matricula)
                                            // Remover dados temporários
                                            database.child("registration").child(tempKey).removeValue()
                                        } else {
                                            Toast.makeText(this, "Falha ao atualizar perfil.", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                } else {
                                    Toast.makeText(this, "Falha ao cadastrar usuário.", Toast.LENGTH_LONG).show()
                                }
                            }
                    } else {
                        Toast.makeText(this, "Falha ao salvar dados temporários.", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    private fun saveUserToDatabase(uid: String, name: String, email: String, sector: String, matricula: String) {
        val user = User(uid, name, email, sector, matricula)
        database.child("users").child(uid).setValue(user)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Usuário cadastrado com sucesso!", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Falha ao salvar informações adicionais.", Toast.LENGTH_LONG).show()
                }
            }
    }
}

data class User(val uid: String, val name: String, val email: String, val sector: String, val matricula: String)