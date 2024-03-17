package com.teamproject2k.facebooklogin

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.GraphRequest
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.teamproject2k.facebooklogin.ui.theme.FacebookLoginTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class MainActivity : ComponentActivity() {
    private var isUserLoggedIn = mutableStateOf(false)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isUserLoggedIn.value = Firebase.auth.currentUser != null
        setContent {
            FacebookLoginTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val sharedPreferenceHelper = SharedPreferenceHelper(this)
                    if (isUserLoggedIn.value) {
                        UserProfileSection(isUserLoggedIn, sharedPreferenceHelper)
                    } else {
                        UserProfileSectionWithoutLogin(isUserLoggedIn, sharedPreferenceHelper)
                    }
                }
            }
        }
    }
}


@Composable
fun UserProfileSection(
    isUserLoggedIn: MutableState<Boolean>,
    sharedPreferences: SharedPreferenceHelper
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = sharedPreferences.userProfilePicture,
                contentDescription = stringResource(R.string.profile_picture),
                modifier = Modifier
                    .clip(CircleShape)
                    .size(80.dp)
                    .background(Color.Black)

            )
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(
                    text = "${sharedPreferences.userFirstName} ${sharedPreferences.userLastName}",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = sharedPreferences.userEmailAddress,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                    fontSize = 14.sp
                )

            }
        }
        Button(
            onClick = {
                Firebase.auth.signOut()
                isUserLoggedIn.value = false
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            Text(text = "Logout")
        }
    }
}


@Composable
fun UserProfileSectionWithoutLogin(
    isUserLoggedIn: MutableState<Boolean>,
    sharedPreferences: SharedPreferenceHelper
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Hi, Please Login to continue using our app",
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(16.dp))
        FacebookLoginButton(
            sharedPreferences = sharedPreferences,
            onAuthCancelled = {
                Toast.makeText(context, "Authentication Cancelled", Toast.LENGTH_SHORT).show()
            },
            onAuthError = {
                isUserLoggedIn.value = false
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            }
        ) {
            Toast.makeText(context, "Logged in successfully", Toast.LENGTH_SHORT).show()
            isUserLoggedIn.value = true
        }
    }
}

@Preview
@Composable
fun A() {
    UserProfileSectionWithoutLogin(
        isUserLoggedIn = mutableStateOf(false),
        SharedPreferenceHelper(LocalContext.current)
    )
}

@Composable
fun FacebookLoginButton(
    sharedPreferences: SharedPreferenceHelper,
    onAuthCancelled: () -> Unit,
    onAuthError: (String) -> Unit,
    onAuthSuccess: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val loginManager = LoginManager.getInstance()
    val callbackManager = remember { CallbackManager.Factory.create() }
    val launcher = rememberLauncherForActivityResult(
        loginManager.createLogInActivityResultContract(callbackManager, null)
    ) {
        // nothing to do. handled in FacebookCallback
    }
    Button(onClick = {
        launcher.launch(listOf("email", "public_profile"))
    }) {
        Text(text = "Login With Facebook")
    }

    DisposableEffect(Unit) {
        loginManager.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onCancel() {
                onAuthCancelled()
            }

            override fun onError(error: FacebookException) {
                onAuthError(error.localizedMessage ?: "Some error occurred")
            }

            override fun onSuccess(result: LoginResult) {
                scope.launch {
                    val token = result.accessToken.token
                    val credential = FacebookAuthProvider.getCredential(token)
                    Firebase.auth.signInWithCredential(credential).await()
                    val request =
                        GraphRequest.newMeRequest(result.accessToken) { responseJson, response ->
                            if (response?.error != null) {
                                Log.e("abc", "onSuccess: ${response.error?.errorMessage}")
                                Firebase.auth.signOut()
                                onAuthError(response.error?.errorMessage ?: "Some error occurred")
                            } else {
                                Log.e("abc", "onSuccess: $responseJson")
                                sharedPreferences.userFirstName =
                                    responseJson?.optString("first_name") ?: ""
                                sharedPreferences.userLastName =
                                    responseJson?.optString("last_name") ?: ""
                                sharedPreferences.userEmailAddress =
                                    responseJson?.optString("email") ?: ""
                                sharedPreferences.userProfilePicture =
                                    responseJson?.getJSONObject("picture")?.getJSONObject("data")
                                        ?.optString("url") ?: ""
                                onAuthSuccess()
                            }
                        }
                    val parameters = Bundle()
                    parameters.putString("fields", "first_name,last_name,gender,email,picture")
                    request.parameters = parameters
                    request.executeAsync()
                }
            }
        })

        onDispose {
            loginManager.unregisterCallback(callbackManager)
        }
    }
}

