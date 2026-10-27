@file:OptIn(ExperimentalMaterial3Api::class)

package com.fabiancg.bottomappbar.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fabiancg.bottomappbar.data.Users
import com.fabiancg.bottomappbar.data.Validators
import com.fabiancg.bottomappbar.ui.AppViewModel
import com.fabiancg.bottomappbar.ui.components.Avatar
import com.fabiancg.bottomappbar.ui.components.ValidatedTextField
import com.fabiancg.bottomappbar.ui.theme.MpazGold
import com.fabiancg.bottomappbar.ui.theme.MpazNavy
import com.fabiancg.bottomappbar.ui.theme.SuccessFg

@Composable
fun AuthPanel(vm: AppViewModel, onAuthenticated: () -> Unit, title: String = "Acceso a MPAZ Ticket") {
    var tab by rememberSaveable { mutableIntStateOf(0) }

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                color = MpazNavy, modifier = Modifier.weight(1f))
            DemoAccountsMenu(onPick = { mail ->
                tab = 0
                email = mail
                password = Users.DEMO_PASSWORD
            })
        }

        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf("Iniciar sesión", "Registrarse").forEachIndexed { i, label ->
                SegmentedButton(
                    selected = tab == i,
                    onClick = { tab = i },
                    shape = SegmentedButtonDefaults.itemShape(index = i, count = 2),
                    label = { Text(label) }
                )
            }
        }

        AnimatedContent(
            targetState = tab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "auth_tab"
        ) { current ->
            if (current == 0) {
                LoginForm(
                    vm = vm,
                    email = email,
                    onEmail = { email = it },
                    password = password,
                    onPassword = { password = it },
                    onAuthenticated = onAuthenticated
                )
            } else {
                RegisterForm(vm = vm, onAuthenticated = onAuthenticated)
            }
        }
    }
}

@Composable
private fun DemoAccountsMenu(onPick: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, contentPadding = ButtonDefaults.TextButtonContentPadding) {
            Icon(Icons.Filled.People, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(" Cuentas demo")
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Text(
                "Datos ficticios · contraseña: ${Users.DEMO_PASSWORD}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
            HorizontalDivider()
            Users.demoLogin.forEach { u ->
                DropdownMenuItem(
                    leadingIcon = { Avatar(u.iniciales, Modifier.size(32.dp)) },
                    text = {
                        Column {
                            Text(u.nombre, fontWeight = FontWeight.SemiBold)
                            Text("${u.role.label} · ${u.email}", style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    onClick = {
                        expanded = false
                        onPick(u.email)
                    }
                )
            }
        }
    }
}

@Composable
private fun LoginForm(
    vm: AppViewModel,
    email: String,
    onEmail: (String) -> Unit,
    password: String,
    onPassword: (String) -> Unit,
    onAuthenticated: () -> Unit
) {
    var intento by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val emailError = Validators.email(email)
    val passError = Validators.passwordLogin(password)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ValidatedTextField(
            value = email,
            onValueChange = { onEmail(it.take(60)); error = null },
            label = "Correo",
            error = emailError,
            showError = intento,
            leadingIcon = Icons.Filled.Email,
            keyboardType = KeyboardType.Email
        )
        ValidatedTextField(
            value = password,
            onValueChange = { onPassword(it.take(30)); error = null },
            label = "Contraseña",
            error = error ?: passError,
            showError = intento || error != null,
            leadingIcon = Icons.Filled.Lock,
            keyboardType = KeyboardType.Password,
            isPassword = true,
            showValid = false
        )
        Button(
            onClick = {
                intento = true
                if (emailError == null && passError == null) {
                    error = vm.login(email, password)
                    if (error == null) onAuthenticated()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
            Text("  Ingresar")
        }
        Text(
            "Pueden ingresar funcionarios y administradores. Usa el menú «Cuentas demo» para probar.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RegisterForm(vm: AppViewModel, onAuthenticated: () -> Unit) {
    var nombre by rememberSaveable { mutableStateOf("") }
    var cargo by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var telefono by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var intento by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    val eNombre = Validators.nombre(nombre)
    val eCargo = Validators.cargo(cargo)
    val eEmail = Validators.emailRegistro(email, Users.emailExists(email))
    val eTel = Validators.telefono(telefono)
    val ePass = Validators.nuevaPassword(password)
    val eConfirm = Validators.confirmacion(password, confirm)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "El registro crea una cuenta con perfil Funcionario. Las cuentas de administrador las entrega la escuela.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ValidatedTextField(
            value = nombre, onValueChange = { nombre = it.take(60); error = null },
            label = "Nombre y apellido *", error = eNombre, showError = intento || nombre.isNotEmpty(),
            leadingIcon = Icons.Filled.Person
        )
        ValidatedTextField(
            value = cargo, onValueChange = { cargo = it.take(60); error = null },
            label = "Cargo (ej: Docente de 4° Básico) *", error = eCargo, showError = intento || cargo.isNotEmpty(),
            leadingIcon = Icons.Filled.Work
        )
        ValidatedTextField(
            value = email, onValueChange = { email = it.take(60); error = null },
            label = "Correo *", error = eEmail, showError = intento || email.isNotEmpty(),
            leadingIcon = Icons.Filled.Email, keyboardType = KeyboardType.Email
        )
        ValidatedTextField(
            value = telefono, onValueChange = { telefono = it.take(16); error = null },
            label = "Teléfono (ej: 9 1234 5678) *", error = eTel, showError = intento || telefono.isNotEmpty(),
            leadingIcon = Icons.Filled.Phone, keyboardType = KeyboardType.Phone
        )
        ValidatedTextField(
            value = password, onValueChange = { password = it.take(30); error = null },
            label = "Contraseña (letras y números) *", error = ePass, showError = intento || password.isNotEmpty(),
            leadingIcon = Icons.Filled.Lock, keyboardType = KeyboardType.Password, isPassword = true
        )
        ValidatedTextField(
            value = confirm, onValueChange = { confirm = it.take(30); error = null },
            label = "Repite la contraseña *", error = eConfirm, showError = intento || confirm.isNotEmpty(),
            leadingIcon = Icons.Filled.Lock, keyboardType = KeyboardType.Password, isPassword = true
        )
        AnimatedVisibility(error != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text("  ${error ?: ""}", color = MaterialTheme.colorScheme.error)
            }
        }
        Button(
            onClick = {
                intento = true
                if (listOf(eNombre, eCargo, eEmail, eTel, ePass, eConfirm).all { it == null }) {
                    error = vm.register(nombre, cargo, email, telefono, password, confirm)
                    if (error == null) onAuthenticated()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.PersonAdd, contentDescription = null)
            Text("  Crear cuenta de funcionario")
        }
    }
}

@Composable
fun AuthDialog(vm: AppViewModel, title: String, message: String, onDismiss: () -> Unit, onAuthenticated: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .imePadding()
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                        color = MpazNavy, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Cerrar") }
                }
                Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider()
                AuthPanel(vm = vm, onAuthenticated = onAuthenticated, title = "Identifícate")
            }
        }
    }
}

@Composable
fun LogoutScreen(vm: AppViewModel, onCancel: () -> Unit, onGoHome: () -> Unit, onLogin: () -> Unit) {
    var cerrada by rememberSaveable { mutableStateOf(false) }
    val user = vm.currentUser

    Box(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(targetState = cerrada || user == null, label = "logout") { terminado ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (terminado) {
                    var show by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { show = true }
                    AnimatedVisibility(show, enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn()) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SuccessFg, modifier = Modifier.size(88.dp))
                    }
                    Text(if (cerrada) "Sesión cerrada" else "No hay una sesión activa",
                        style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Tus reportes y mensajes quedan guardados en este dispositivo. " +
                                "Puedes seguir usando la página de inicio como invitado.",
                        textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = onGoHome, modifier = Modifier.fillMaxWidth()) { Text("Ir al inicio") }
                    OutlinedButton(onClick = onLogin, modifier = Modifier.fillMaxWidth()) { Text("Iniciar sesión nuevamente") }
                } else {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = MpazGold, modifier = Modifier.size(72.dp))
                    Text("¿Cerrar sesión?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Saldrás de la cuenta de ${user?.nombre ?: ""} (${user?.role?.label ?: ""}).",
                        textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            vm.logout()
                            cerrada = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Sí, cerrar sesión") }
                    OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
                }
            }
        }
    }
}