package application.controladores;

import application.modelos.*;
import com.calendarfx.model.Calendar;
import com.calendarfx.model.CalendarEvent;
import com.calendarfx.model.CalendarSource;
import com.calendarfx.model.Entry;
import com.calendarfx.view.*;
import com.calendarfx.view.page.DayPage;
import com.jfoenix.controls.*;
import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;
import com.lynden.gmapsfx.GoogleMapView;
import com.lynden.gmapsfx.MapComponentInitializedListener;
import com.lynden.gmapsfx.javascript.object.*;
import com.lynden.gmapsfx.service.geocoding.GeocoderStatus;
import com.lynden.gmapsfx.service.geocoding.GeocodingResult;
import com.lynden.gmapsfx.service.geocoding.GeocodingService;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.joda.time.Hours;
import org.joda.time.Period;

import java.io.IOException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class controladorVistaGeneral implements Initializable, MapComponentInitializedListener {

    private modelo modelo;
    private Usuario usuario;
    private final List<Label> labelMessages = new ArrayList<>();
    private final List<Label> labelMessagesInicio = new ArrayList<>();
    private final List<Label> labelFAQ = new ArrayList<>();
    private final List<String> uniqueMessageIDS = new ArrayList<>();
    private controladorVistaGeneral cp;
    private ConexionBBDD conexionBBDD; // new ConexionBBDD();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"); // Formato que le daremos a la fecha
    private ArrayList<Usuario> relatedUsers;

    public void initModelo(modelo modelo_, Usuario usuario_, controladorVistaGeneral cp_, String tipoVista, ConexionBBDD conexionBBDD) {
        if (this.modelo != null) {
            throw new IllegalStateException("Model can only be initialized once");
        }
        this.modelo = modelo_;
        this.usuario = usuario_;
        this.cp = cp_;
        this.conexionBBDD = conexionBBDD;
        this.relatedUsers = modelo.usuariosRelacionados(usuario);

        Timer timer = new Timer();
        // Comprueba cada minuto
        if (usuario.getRol().equals("paciente"))
            timer.schedule(new comprobacionAlertas(this, modelo, conexionBBDD, usuario.getID_User(), "paciente"), 0, 60000);
        else
            timer.schedule(new comprobacionAlertas(this, modelo, conexionBBDD, usuario.getRol(), relatedUsers), 0, 60000);

        modelo.setMessages(conexionBBDD.getMensajesDeUsuario(usuario.getID_User()));

        aniadirPreguntasFrecuentes();

        // Datos pestaña inicio
        labelNombreInicio.setText(usuario.getName());
        labelApellidosInicio.setText(usuario.getSurnames());
        labelRolInicio.setText(usuario.getRol());
        labelUsernameInicio.setText(usuario.getUser());
        labelFechaNacimientoInicio.setText(usuario.getDOB()+"");
        labelEdadInicio.setText(usuario.getAge() + "");
        labelDNIInicio.setText(usuario.getDNI());
        labelTelefonoInicio.setText(usuario.getTelephone() + "");

        // Establecemos la foto del usuario en la pestaña de Inicio
        if (usuario.getPhoto() == null)
            userImageViewInicio.setImage(new Image("@..\\..\\resources\\fotos\\user.png"));
        else
        	userImageViewInicio.setImage(new Image(usuario.getPhoto()));

        // Creamos las listas de usuarios y mensajes
        crearTreeTableView(treeTableViewUsuarios, false);
        crearTreeTableView(treeTableViewRegistros, true);
        crearTreeTableView(treeTableViewLocalizacion, true);
        
        crearTreeTableViewMensajes();

        // Comprobamos mensajes nuevos
        comprobarMensajesNuevos();

        if (tipoVista.equals("general")) tabPaneGeneral.getTabs().remove(4, 6);

        // Poblamos el calendario con eventos
        initCalendario();

    } // initModelo()

    // -------------------- Tab Inicio --------------------

    @FXML private JFXTabPane tabPaneGeneral;

    @FXML private Tab tabInicio;

    @FXML private ImageView userImageViewInicio;

    @FXML private Label labelNombreInicio;

    @FXML private Label labelApellidosInicio;

    @FXML private Label labelUsernameInicio;

    @FXML private Label labelRolInicio;

    @FXML private Label labelFechaNacimientoInicio;

    @FXML private Label labelEdadInicio;

    @FXML private Label labelDNIInicio;

    @FXML private Label labelTelefonoInicio;

    @FXML private JFXButton cerrarSesionBtn;

    @FXML private ScrollPane scrollPaneMensajesInicio;

    @FXML private AnchorPane conversacionMensajesInicio;

    @FXML private VBox vboxConversacionMensajesInicio;

    @FXML private AgendaView agendaViewInicio;

    @FXML private ScrollPane scrollPaneFAQ;

    @FXML private AnchorPane apaneFAQ;

    @FXML private VBox vboxFAQ;



    // -------------------- Tab Calendario --------------------

    @FXML private Tab tabCalendario;

    @FXML private DayPage calendario;
    



    // -------------------- Tab Usuarios --------------------

    @FXML private Tab tabUsuarios;

    @FXML private Label seleccionaUsuarioLabel;

    @FXML private JFXTextField filtrarUsuarioTFieldUsuarios;

    @FXML private JFXTreeTableView<usuarioTTView> treeTableViewUsuarios;

    @FXML private VBox datosUsuarioVBox;

    @FXML private ImageView userImageViewUsuarios;

    @FXML private Label labelNombreUsuarios;

    @FXML private Label labelApellidosUsuarios;

    @FXML private Label labelTelefonoUsuarios;

    @FXML private Label labelRolUsuarios;

    @FXML private Label labelEdadUsuarios;

    @FXML private JFXTextField destinatarioJFXTextFieldUsuarios;

    @FXML private JFXTextField asuntoJFXTextFieldUsuarios;

    @FXML private JFXTextArea mensajeJFXTextFieldUsuarios;

    @FXML private HBox generarTicketHBox;

    @FXML private JFXButton cancelarTicketBttnMensajes;

    @FXML private JFXButton crearTicketBttnMensajes;



    // -------------------- Tab Mensajes --------------------

    @FXML private Tab tabMensajes;

    @FXML private Label seleccionaMensajeLabelMensajes;

    @FXML private JFXTextField filtrarMensajeTFieldMensajes;

    @FXML private JFXTreeTableView<messageTTView> treeTableViewMensajes;

    @FXML private VBox datosVBoxMensajes;

    @FXML private JFXTextField asuntoJFXTextFieldMensajes;

    @FXML private JFXTextField destinatarioJFXTextFieldMensajes;

    @FXML private JFXTextField idTicketJFXTextFieldMensajes;

    @FXML private ScrollPane scrollPaneMensajes;

    @FXML private AnchorPane conversacionMensajes;

    @FXML private VBox vboxConversacionMensajes;

    @FXML private JFXButton botonResponderTicket;



    // -------------------- Tab Registros --------------------

    @FXML private Tab tabRegistros;

    @FXML private JFXTreeTableView<usuarioTTView> treeTableViewRegistros;

    @FXML private DatePicker calendarioSensores;

    @FXML private ScrollPane scrollPaneRegistros;

    @FXML private AnchorPane apaneRegistros;

    @FXML private VBox vboxRegistros_apane;

    @FXML private LineChart<Double, Double> graficaTemperatura;

    @FXML private ScatterChart<Double, Double> graficaMagnetico;

    @FXML private PieChart graficaPresion;

    @FXML private Label horasDurmiendo;

    @FXML private StackedBarChart<Double, Double> graficaGas;

    private final ObservableList<PieChart.Data> detalles = FXCollections.observableArrayList();



    // -------------------- Tab Localizacion --------------------

    @FXML private Tab tabLocalizacion;

    @FXML private JFXTreeTableView<usuarioTTView> treeTableViewLocalizacion;

    @FXML private TextField addressTextField;

    @FXML private JFXButton buttonActualizarUbicacion;

    @FXML private JFXButton buttonUbicacionCasa;

    @FXML private GoogleMapView mapView;

    private GoogleMap map;

    private GeocodingService geocodingService;

    private final StringProperty address = new SimpleStringProperty();




    // -------------------- Metodos tab Inicio --------------------

    public void aniadirPreguntasFrecuentes() {
        int i = 0;
        String[] preguntasYRespuestas = {"1. ¿Cómo puedo enviar un mensaje a mi médico?\n", "En la pestaña \"Mensajes\" en la parte inferior izquierda hay que pulsar el botón \"Crear Nuevo Ticket\" ahi te aparece para introducir el destinatario, asunto y mensaje."
                , "2. ¿Dónde puedo cerrar sesión?\n", "En la pestaña de \"Inicio\" en la parte inferior izquierda, hay que pulsar el botón\"Cerrar Sesión\"."
                , "3. ¿Puedo buscar un Usuario por su nombre o apellido?\n", "Sí. En la pestaña de Usuarios, arriba a la izquierda pinchas donde pone buscar e introduces el nombre o apellido."
                , "4. ¿Dónde puedo ver todos mis mensajes?\n", "En la pestaña \"Mensajes\" sale la lista de mensajes recibidos y enviados, pudiendo leerlos pinchando en ellos."
                , "5. Ventajas en la lista de Usuarios.\n", "En la lista de usuarios, se pueden ordenar alfabéticamente tanto los nombres, los apellidos y los roles."
                , "6. ¿Dónde puedo ver mis datos personales?\n", "En la pestaña \"Inicio\" debajo de la foto de perfil se encuentran todo sus datos personales."
                , "7. ¿Cómo puedo buscar a un usuario?\n", "En la pestaña \"Usuarios\" una vez seleccionado el usuario de la lista, se mostrará a la derecha de la pantalla toda la información de los datos relativos."};
        for (String PyR : preguntasYRespuestas) {
            labelFAQ.add(new Label(PyR));
            if (i % 2 == 0)
                detailLabel(labelFAQ.get(i), Font.font("Century Gothic", FontWeight.BOLD, 20), 922, new Insets(0,0,0,10));
            else
                detailLabel(labelFAQ.get(i), Font.font("Century Gothic", 20), 922, new Insets(0,0,0,20));

            vboxFAQ.getChildren().add(labelFAQ.get(i));
            vboxFAQ.setSpacing(10);
            i++;
        }
    } // aniadirPreguntasFrecuentes()

    public void comprobarMensajesNuevos() {
        // Borramos la conversacion en casa de que hubiese una seleccionada para poder introducir la siguiente
        labelMessagesInicio.clear();
        vboxConversacionMensajesInicio.getChildren().clear();
        try {
            int i = 0;
            // En este caso no usamos una lambda para no tener que usar un AtomicInteger, por lo tanto simplificando el codigo
            for (Message mensaje : modelo.getMessages()) {
                // Si no esta leido y el senderID coincide con el del usuario
                if (!mensaje.getRead() && mensaje.getReceiverID() == usuario.getID_User()) {
                    ResultSet sender = conexionBBDD.selectUserFromID(mensaje.getSenderID());
                    sender.next(); // Sabemos que si que habra un resultado
                    labelMessagesInicio.add(new Label("- Asunto: " + mensaje.getSubject() + " || De parte de: " + sender.getString("Name")
                            + " " + sender.getString("Surnames")));
                    labelMessagesInicio.get(i).setPrefWidth(1330);
                    labelMessagesInicio.get(i).setWrapText(true);
                    labelMessagesInicio.get(i).setFont(new Font("Century Gothic", 26));
                    labelMessagesInicio.get(i).setPadding(new Insets(0, 0, 0, 20));
                    vboxConversacionMensajesInicio.getChildren().add(labelMessagesInicio.get(i));
                    vboxConversacionMensajesInicio.setSpacing(15);
                    i++;
                }
            }
        } catch (SQLException sqle) {
            System.err.println(sqle.getClass().getName() + ": " + sqle.getMessage());
        }
        if (labelMessagesInicio.size() == 0) {
            labelMessagesInicio.add(new Label(" - No tiene mensajes nuevos"));
            labelMessagesInicio.get(0).setFont(new Font("Century Gothic", 26));
            vboxConversacionMensajesInicio.getChildren().add(labelMessagesInicio.get(0));
        }
    } // comprobarMensajesNuevos()

    public void crearAlertaPaciente(String cuerpoMensaje) {
        modelo.createAlert("Informacion", cuerpoMensaje);
    }

    @FXML
    void cerrarSesion(ActionEvent event) throws IOException {
        Stage stageBttnBelongsTo = (Stage) cerrarSesionBtn.getScene().getWindow();
        FXMLLoader loaderLogin = new FXMLLoader(getClass().getResource("/application/vistas/vistaLogin.fxml"));
        Parent rootLogin;
        rootLogin = loaderLogin.load();
        controladorLogin contrLogin = loaderLogin.getController();
        contrLogin.initModelo(modelo, usuario);
        stageBttnBelongsTo.setScene(new Scene(rootLogin));
    } // cerrarSesion()

    // -------------------- Fin metodos Tab inicio --------------------








    // -------------------- Metodos tab Calendario --------------------

    @SuppressWarnings("rawtypes")
    public void initCalendario(){
        Calendar calendario_citas = new Calendar("Citas");
        Calendar calendario_personal = new Calendar("Personal");

        HashMap<String, Vector<entradaCalendario>> entradasCal = conexionBBDD.recogerEntradasUsuario(usuario.getID_User());
        for (Map.Entry<String, Vector<entradaCalendario>> entry : entradasCal.entrySet()) {
            for (entradaCalendario single_entry : entry.getValue()) {
                if (entry.getKey().equals("Personal"))
                    calendario_personal.addEntry(single_entry.getEntradaCal());
                else
                    calendario_citas.addEntry(single_entry.getEntradaCal());
            }
        }

        // De esta forma si un medico le pone una cita el no podria hacerle cambios, ya que si pudiese entonces tambien le afectarian estos cambios
        // al medico y podria ser critico
        if (usuario.getRol().equals("paciente"))
            calendario_citas.setReadOnly(true);

        calendario_personal.setStyle(Calendar.Style.STYLE2); // rojo claro
        calendario_citas.setStyle(Calendar.Style.STYLE1); // verde

        CalendarSource calendarSourceTasks = new CalendarSource("Eventos");
        calendarSourceTasks.getCalendars().addAll(calendario_citas, calendario_personal);

        calendario.getCalendarSources().set(0, calendarSourceTasks);

        // Creamos el menu que sale al dar doble click en una entrada, y añadimos la opcion de seleccionar usuarios a los que añadir el evento
        calendario.setEntryContextMenuCallback((param) -> {
            EntryViewBase<?> entryView = param.getEntryView();
            Entry<?> entry = entryView.getEntry();
            ContextMenu contextMenu = new ContextMenu();
            MenuItem informationItem = new MenuItem(Messages.getString("DateControl.MENU_ITEM_INFORMATION"));
            informationItem.setOnAction((evt) -> {
                Callback<DateControl.EntryDetailsParameter, Boolean> detailsCallback = calendario.getEntryDetailsCallback();
                if (detailsCallback != null) {
                    ContextMenuEvent ctxEvent = param.getContextMenuEvent();
                    DateControl.EntryDetailsParameter entryDetailsParam = new DateControl.EntryDetailsParameter(ctxEvent, calendario, entryView.getEntry(), calendario, ctxEvent.getScreenX(), ctxEvent.getScreenY());
                    detailsCallback.call(entryDetailsParam);
                }

            });
            contextMenu.getItems().add(informationItem);
            String stylesheet = CalendarView.class.getResource("calendar.css").toExternalForm();
            Menu calendarMenu = new Menu(Messages.getString("DateControl.MENU_CALENDAR"));
            Iterator var8 = calendario.getCalendars().iterator();

            while(var8.hasNext()) {
                Calendar calendar = (Calendar)var8.next();
                RadioMenuItem calendarItem = new RadioMenuItem(calendar.getName());
                calendarItem.setOnAction((evt) -> {
                    entry.setCalendar(calendar);
                });
                calendarItem.setDisable(calendar.isReadOnly());
                calendarItem.setSelected(calendar.equals(param.getCalendar()));
                calendarMenu.getItems().add(calendarItem);
                StackPane graphic = new StackPane();
                graphic.getStylesheets().add(stylesheet);
                Rectangle icon = new Rectangle(10.0D, 10.0D);
                icon.setArcHeight(2.0D);
                icon.setArcWidth(2.0D);
                icon.getStyleClass().setAll(calendar.getStyle() + "-icon");
                graphic.getChildren().add(icon);
                calendarItem.setGraphic(graphic);
            }

            if (usuario.getRol().equals("medico")) {
                Menu usuariosMenu = new Menu("Pacientes");
                Iterator relUsers = relatedUsers.iterator();

                while (relUsers.hasNext()) {
                    Usuario user = (Usuario)relUsers.next();
                    if (user.getRol().equals("paciente")) {
                        RadioMenuItem listaPacientes = new RadioMenuItem(user.getName()+ " "+user.getSurnames());
                        listaPacientes.setOnAction(event -> {
                            conexionBBDD.insertarEntrada(user.getID_User(), param.getEntry().getTitle(), param.getEntry().getStartAsLocalDateTime(),
                                                         param.getEntry().getEndAsLocalDateTime(), param.getCalendar().getName());
                        });
                        listaPacientes.setSelected(conexionBBDD.searchEntryInSharedCalendary(user.getID_User(), param.getEntry().getTitle()));

                        StackPane graphic = new StackPane();
                        Circle icon = new Circle(5.0D, Paint.valueOf("#0163af")); // El azul del tabpane
                        graphic.getChildren().add(icon);
                        listaPacientes.setGraphic(graphic);

                        usuariosMenu.getItems().add(listaPacientes);
                    }
                }

                contextMenu.getItems().add(usuariosMenu);
            }

            calendarMenu.setDisable(param.getCalendar().isReadOnly());
            contextMenu.getItems().add(calendarMenu);
            if ((Boolean)calendario.getEntryEditPolicy().call(new DateControl.EntryEditParameter(calendario, entry, DateControl.EditOperation.DELETE))) {
                MenuItem delete = new MenuItem(Messages.getString("DateControl.MENU_ITEM_DELETE"));
                contextMenu.getItems().add(delete);
                delete.setDisable(param.getCalendar().isReadOnly());
                delete.setOnAction((evt) -> {
                    Calendar calendar = entry.getCalendar();
                    if (!calendar.isReadOnly()) {
                        if (entry.isRecurrence()) {
                            entry.getRecurrenceSourceEntry().removeFromCalendar();
                        } else {
                            entry.removeFromCalendar();
                        }
                    }

                });
            }

            return contextMenu;
        });

        EventHandler<CalendarEvent> handler = event -> {
            if (event.isEntryAdded()) {
                // Insertamos la entrada en la BBDD
                conexionBBDD.insertarEntrada(usuario.getID_User(), event.getEntry().getTitle(), event.getEntry().getStartAsLocalDateTime(),
                        event.getEntry().getEndAsLocalDateTime(), event.getCalendar().getName());

                // El unico sitio donde hace falta añadir es aqui, puesto que el resto de eventos dependen de este hashmap
                if (event.getEntry().getCalendar().getName().equals("Citas"))
                    entradasCal.get("Citas").add(new entradaCalendario(conexionBBDD.idUltimaEntrada(), event.getEntry()) );
                else
                    entradasCal.get("Personal").add(new entradaCalendario(conexionBBDD.idUltimaEntrada(), event.getEntry()) );
            }

            else if (event.isEntryRemoved()) {
                Iterator<entradaCalendario> iter = entradasCal.get(event.getOldCalendar().getName()).iterator();

                while (iter.hasNext()) {
                    entradaCalendario entryIter = iter.next();
                    if (entryIter.getEntradaCal().equals(event.getEntry())) {
                        conexionBBDD.removeEntry(entryIter.getID_Entrada());
                        break;
                    }
                }
            }

            // Mientras cambia algun intervalo (startDateTime o endDateTime cambian)
            else if (event.getEventType().getName().equals("ENTRY_INTERVAL_CHANGED")) {
                Iterator<entradaCalendario> iter = entradasCal.get(event.getCalendar().getName()).iterator();

                while (iter.hasNext()) {
                    entradaCalendario entryIter = iter.next();
                    if (entryIter.getEntradaCal().equals(event.getEntry())) {
                        conexionBBDD.updateEntryInterval(event.getEntry().getStartAsLocalDateTime().toString(),
                                event.getEntry().getEndAsLocalDateTime().toString(), entryIter.getID_Entrada());
                        break;
                    }
                }
            }

            else if (event.getEventType().getName().equals("ENTRY_TITLE_CHANGED")) {
                Iterator<entradaCalendario> iter = entradasCal.get(event.getCalendar().getName()).iterator();

                while (iter.hasNext()) {
                    entradaCalendario entryIter = iter.next();
                    if (entryIter.getEntradaCal().equals(event.getEntry())) {
                        String sql = "UPDATE `entradas_calendario` SET Title = ? WHERE ID_Entry = ?;";
                        conexionBBDD.updateEntry(sql, event.getEntry().getTitle(), entryIter.getID_Entrada());

                        // Tenemos que fire el event manualmente al cambiar el titulo (supongo que por la implementacion de libreria)
                        calendario.getAgendaView().getListView().refresh();
                        break;
                    }
                }
            }

            else if (event.getEventType().getName().equals("ENTRY_CALENDAR_CHANGED")) {
                // Old Calendar al cambiar de un calendario a otro
                Iterator<entradaCalendario> iter = entradasCal.get(event.getOldCalendar().getName()).iterator();

                while (iter.hasNext()) {
                    entradaCalendario entryIter = iter.next();
                    if (entryIter.getEntradaCal().equals(event.getEntry())) {
                        String sql = "UPDATE `entradas_calendario` SET Calendario = ? WHERE ID_Entry = ?;";
                        conexionBBDD.updateEntry(sql, event.getEntry().getCalendar().getName(), entryIter.getID_Entrada());
                        break;
                    }
                }
            }

            else if (event.getEventType().getName().equals("ENTRY_FULL_DAY_CHANGED")) {
                System.out.println("Entry full day changed");
            }

            else if (event.getEventType().getName().equals("ENTRY_LOCATION_CHANGED")) {
                System.out.println("Location changed");
            }

            else if (event.getEventType().getName().equals("ENTRY_RECURRENCE_RULE_CHANGED")) {
                System.out.println("Recurrence rule modified"); // Mirar docs
            }

            // ENTRY_USER_OBJECT_CHANGED (creo que no hace falta)

        };
        calendario_personal.addEventHandler(handler);
        calendario_citas.addEventHandler(handler);

        // Sincronizamos el calendar source del calendar principal con la agenda view del inicio, de tal forma que cualquier cambio se refleje en este
        agendaViewInicio.getCalendarSources().setAll(calendario.getCalendarSources());

    } // initCalendario()

    // -------------------- Fin metodos tab Calendario --------------------








    // -------------------- Metodos tab Usuarios --------------------

    @FXML
    void filterUsersUsuario(KeyEvent event) {
        treeTableViewUsuarios.setPredicate(usuarioTreeItem -> usuarioTreeItem.getValue().getName().get().toLowerCase().startsWith(filtrarUsuarioTFieldUsuarios.getText().toLowerCase()) ||
                usuarioTreeItem.getValue().getSurname().get().toLowerCase().startsWith(filtrarUsuarioTFieldUsuarios.getText().toLowerCase())
                || usuarioTreeItem.getValue().getRolUsuario().get().toLowerCase().startsWith(filtrarUsuarioTFieldUsuarios.getText().toLowerCase()));
    } // filterUserUsuario()

    public void crearTreeTableView(TreeTableView<usuarioTTView> ttv, boolean soloPacientes) {
        JFXTreeTableColumn<usuarioTTView, String> nombreCol = new JFXTreeTableColumn<>("Nombre");
        JFXTreeTableColumn<usuarioTTView, String> apellidosCol = new JFXTreeTableColumn<>("Apellidos");
        JFXTreeTableColumn<usuarioTTView, String> rolCol = new JFXTreeTableColumn<>("Rol");

        nombreCol.setCellValueFactory(param -> param.getValue().getValue().getName());
        nombreCol.setMinWidth(129);
        nombreCol.setMaxWidth(129);
        apellidosCol.setCellValueFactory(param -> param.getValue().getValue().getSurname());
        apellidosCol.setMinWidth(189);
        apellidosCol.setMaxWidth(189);
        rolCol.setCellValueFactory(param -> param.getValue().getValue().getRolUsuario());
        rolCol.setMinWidth(159);
        rolCol.setMaxWidth(159);

        ObservableList<usuarioTTView> users = FXCollections.observableArrayList();
        
        // Añadimos los usuarios
        if (soloPacientes) {
        	for (Usuario user : relatedUsers)
        		if (user.getRol().equals("paciente")) 
                    users.add(new usuarioTTView(user.getID_User(), user.getName(), user.getSurnames(), user.getRol(), user.getDOB(), user.getAge(), user.getPhoto(), user.getAdress(),  user.getTelephone()));
		} else {
	        // Añadimos los usuarios
			for (Usuario user : relatedUsers)
				users.add(new usuarioTTView(user.getID_User(), user.getName(), user.getSurnames(), user.getRol(), user.getDOB(), user.getAge(), user.getPhoto(), user.getAdress(),  user.getTelephone()));
		}
 
        TreeItem<usuarioTTView> root = new RecursiveTreeItem<>(users, RecursiveTreeObject::getChildren);

        // TTV Usuarios
        ttv.getColumns().add(0, nombreCol);
        ttv.getColumns().add(1, apellidosCol);
        ttv.getColumns().add(2, rolCol);
        ttv.setRoot(root);
    } // crearTreeTableViewUsuarios()


    @FXML
    void mostrarDatosUsuarios(MouseEvent event) {
        // Cambiamos los datos del usuario mientras se haya seleccionado uno
        if (treeTableViewUsuarios.getSelectionModel().getSelectedItem() != null) {
            // Comprobamos si el panel de datos de usuario y creacion de mensajes esta visible
            if (!datosUsuarioVBox.isVisible()) {
                datosUsuarioVBox.setVisible(true);
                seleccionaUsuarioLabel.setVisible(false);
            }
            // Si esta visible actualizamos los datos del usuario seleccionado
            if (datosUsuarioVBox.isVisible()) {
                labelNombreUsuarios.setText(treeTableViewUsuarios.getSelectionModel().getSelectedItem().getValue().getName().get());
                labelApellidosUsuarios.setText(treeTableViewUsuarios.getSelectionModel().getSelectedItem().getValue().getSurname().get());
                labelRolUsuarios.setText(treeTableViewUsuarios.getSelectionModel().getSelectedItem().getValue().getRolUsuario().get());
                labelTelefonoUsuarios.setText(treeTableViewUsuarios.getSelectionModel().getSelectedItem().getValue().getTelephone().get()+"");
                labelEdadUsuarios.setText(treeTableViewUsuarios.getSelectionModel().getSelectedItem().getValue().getAge().get() + "");
                destinatarioJFXTextFieldUsuarios.setText(treeTableViewUsuarios.getSelectionModel().getSelectedItem().getValue().getName().get() + " "
                        + treeTableViewUsuarios.getSelectionModel().getSelectedItem().getValue().getSurname().get());
                userImageViewUsuarios.setImage(new Image(treeTableViewUsuarios.getSelectionModel().getSelectedItem().getValue().getImagenPerfil().get()));

                // Reseteamos el campo de asunto y textfield en caso de que el usuario no cancelo la respuesta
                asuntoJFXTextFieldUsuarios.clear();
                mensajeJFXTextFieldUsuarios.clear();
            }
        }
    } // mostrarDatosYMensajeUsuarios()

    @FXML
    void crearTicket(ActionEvent event) {
        if (asuntoJFXTextFieldUsuarios.getText().isEmpty()) {
            modelo.createAlert("Cuidado", "Debes de poner un asunto");
        } else if (mensajeJFXTextFieldUsuarios.getText().isEmpty()) {
            modelo.createAlert("Cuidado", "Debes de poner un mensaje");
        } else {
            UUID uniqueKey = UUID.randomUUID();
            Message msg = new Message(usuario.getID_User(), treeTableViewUsuarios.getSelectionModel().getSelectedItem().getValue().getID_User().get(),
                                      asuntoJFXTextFieldUsuarios.getText(), mensajeJFXTextFieldUsuarios.getText(), uniqueKey.toString(), false);

            conexionBBDD.insertarMensaje(msg.getIdTicket(), msg.getMessage(), msg.getSubject(), msg.getSenderID(), msg.getReceiverID());
            modelo.createAlert("Informacion", "Se ha enviado el mensaje correctamente");

            // Borramos los campos para evitar confusion
            asuntoJFXTextFieldUsuarios.clear();
            mensajeJFXTextFieldUsuarios.clear();

            // Actualizamos la lista de mensajes
            treeTableViewMensajes.getRoot().getChildren().add(new TreeItem<>(new messageTTView(msg.getSenderID(), msg.getReceiverID(),
                                                               usuario.getName() + usuario.getSurnames(),
                                                               treeTableViewUsuarios.getSelectionModel().getSelectedItem().getValue().getName().get()
                                                                       + treeTableViewUsuarios.getSelectionModel().getSelectedItem().getValue().getSurname().get(),
                                                               msg.getSubject(), msg.getMessage(), msg.getIdTicket(), msg.getRead(), usuario.getUser())));
            // Junto a los mensajes no leidos
            comprobarMensajesNuevos();
        }
    } // enviarMensajeUsuarios()

    @FXML
    void cancelarTicket(ActionEvent event) {
        if (asuntoJFXTextFieldUsuarios.getText().isEmpty() && mensajeJFXTextFieldUsuarios.getText().isEmpty())
            modelo.createAlert("Informacion", "Primero debe introducir un asunto o un mensaje");
        else {
            asuntoJFXTextFieldUsuarios.clear();
            mensajeJFXTextFieldUsuarios.clear();
        }
    } // cancelarMensajeUsuarios()

    // -------------------- Fin metodos tab Usuarios --------------------








    // -------------------- Metodos tab Mensajes --------------------

    @FXML
    void filterTicketsMensajes(KeyEvent event) {
        treeTableViewMensajes.setPredicate(mensajeTreeItem -> mensajeTreeItem.getValue().getSubject().get().toLowerCase().startsWith(filtrarMensajeTFieldMensajes.getText().toLowerCase()) ||
                mensajeTreeItem.getValue().getIdTicket().get().startsWith(filtrarMensajeTFieldMensajes.getText()));
    } // filterTicketsMensajes()

    public void crearTreeTableViewMensajes() {
        JFXTreeTableColumn<messageTTView, String> idCol = new JFXTreeTableColumn<>("ID Ticket");
        JFXTreeTableColumn<messageTTView, String> senderCol = new JFXTreeTableColumn<>("De");
        JFXTreeTableColumn<messageTTView, String> asuntoCol = new JFXTreeTableColumn<>("Asunto");

        idCol.setCellValueFactory(param -> param.getValue().getValue().getIdTicket());
        idCol.setMinWidth(129);
        idCol.setMaxWidth(129);
        senderCol.setCellValueFactory(param -> param.getValue().getValue().getSender());
        senderCol.setMinWidth(189);
        senderCol.setMaxWidth(189);
        asuntoCol.setCellValueFactory(param -> param.getValue().getValue().getSubject());
        asuntoCol.setMinWidth(159);
        asuntoCol.setMaxWidth(159);

        ObservableList<messageTTView> messages = FXCollections.observableArrayList();

        // Añadimos los mensajes
        try {
            for (Message msg : modelo.getMessages() ) {
                if (!uniqueMessageIDS.contains(msg.getIdTicket())) { // Comprobamos que solo añadimos el ticket, no la conversacion entera
                    ResultSet sender = conexionBBDD.selectUserFromID(msg.getSenderID()), receiver = conexionBBDD.selectUserFromID(msg.getReceiverID());
                    if (sender.next() && receiver.next()) { // Si hay un mensaje
                        messages.add(new messageTTView(msg.getSenderID(), msg.getReceiverID(), sender.getString("Name") + " " + sender.getString("Surnames"),
                                receiver.getString("Name") + " " + receiver.getString("Surnames"), msg.getSubject(), msg.getMessage(), msg.getIdTicket(),
                                msg.getRead(), sender.getString("User")));
                        uniqueMessageIDS.add(msg.getIdTicket());
                    }
                }
            }
        } catch (SQLException sqle) {
            System.err.println(sqle.getClass().getName() + ": " + sqle.getMessage());
        }

        TreeItem<messageTTView> root = new RecursiveTreeItem<>(messages, RecursiveTreeObject::getChildren);
        treeTableViewMensajes.getColumns().add(idCol);
        treeTableViewMensajes.getColumns().add(senderCol);
        treeTableViewMensajes.getColumns().add(asuntoCol);
        treeTableViewMensajes.setRoot(root);
    } // createTreeTableViewMensajes()

    @FXML
    void mostrarTicketMensajes(MouseEvent event) {
        if (treeTableViewMensajes.getSelectionModel().getSelectedItem() != null) { // Cambiamos los datos del mensaje
            // Comprobamos si el panel de mensajes esta visible
            if (!datosVBoxMensajes.isVisible())
                datosVBoxMensajes.setVisible(true);

            // Borramos la conversacion en caso de que hubiese una seleccionada para poder introducir la siguiente
            labelMessages.clear();
            vboxConversacionMensajes.getChildren().clear();
            asuntoJFXTextFieldMensajes.setText(treeTableViewMensajes.getSelectionModel().getSelectedItem().getValue().getSubject().get());
            destinatarioJFXTextFieldMensajes.setText(treeTableViewMensajes.getSelectionModel().getSelectedItem().getValue().getReceiver().get());
            idTicketJFXTextFieldMensajes.setText(treeTableViewMensajes.getSelectionModel().getSelectedItem().getValue().getIdTicket().get());

            // Cojemos todos los mensajes pertenecientes a un ticket
            Vector<Message> messagesInATicket = conexionBBDD.getMensajesDeTicket(idTicketJFXTextFieldMensajes.getText());

            // Marcamos el mensaje/s como leido
            setMsgAsRead(messagesInATicket);

            // Cambiamos el scroll pane para mostrar la lista de mensajes correspondientes al ticket seleccionado
            changeTicketConversation(messagesInATicket);
        }
    } // mostrarTicketMensajes()

    public void setMsgAsRead(Vector<Message> messages) {
        // Cambiamos el mensaje como leido si no lo estaba
        for (Message msg : messages) {
            if (!msg.getRead() && msg.getReceiverID() == usuario.getID_User())
                conexionBBDD.setMsgAsRead(msg);
        }

        // Actualizamos la lista de mensajes
        modelo.setMessages(conexionBBDD.getMensajesDeUsuario(usuario.getID_User()));

        // Borramos el mensaje de la pestaña recordatorios
        comprobarMensajesNuevos();

    } // setMsgAsRead()

    public void changeTicketConversation(Vector<Message> messages) {
        // En este caso no usamos una lambda para no tener que usar un AtomicInteger, por lo tanto simplificando el codigo
        int i = 0;
        for (Message mensaje : messages) {
            VBox vb = new VBox();
            vb.setFillWidth(false);
            vb.setPrefWidth(scrollPaneMensajes.getPrefWidth());

            TextFlow tf = new TextFlow();
            Text t1 = new Text();
            t1.setFont(Font.font("Century Gothic", FontWeight.BOLD, 15));

            labelMessages.add(new Label(mensaje.getMessage()));
            labelMessages.get(i).setMaxWidth(scrollPaneMensajes.getPrefWidth()/2);
            labelMessages.get(i).setWrapText(true);
            labelMessages.get(i).setFont(new Font("Century Gothic", 18));
            if (mensaje.getSenderID() == usuario.getID_User()) {
                t1.setText(usuario.getUser().toUpperCase()+"\n");
                vb.setAlignment(Pos.CENTER_RIGHT);
                labelMessages.get(i).setBackground(new Background(new BackgroundFill(Color.WHEAT, new CornerRadii(5, 5, 5, 5, false), Insets.EMPTY)));
                labelMessages.get(i).setPadding(new Insets(0, 10, 0, 10));
            } else {
                t1.setText(treeTableViewMensajes.getSelectionModel().getSelectedItem().getValue().getSenderUName().get().toUpperCase()+"\n");
                labelMessages.get(i).setBackground(new Background(new BackgroundFill(Color.WHITESMOKE, new CornerRadii(5, 5, 5, 5, false), Insets.EMPTY)));
                labelMessages.get(i).setPadding(new Insets(0,0,0,10));
            }
            tf.getChildren().addAll(t1, labelMessages.get(i));
            vb.getChildren().add(tf);
            vboxConversacionMensajes.getChildren().add(vb);
            vboxConversacionMensajes.setSpacing(15);
            i++;
        }
        // Bajamos el scrollPane hasta el ultimo mensaje
        vboxConversacionMensajes.heightProperty().addListener(observable -> scrollPaneMensajes.setVvalue(1.0));
    } // changeTicketConversation()


    public void crearMensajeYResponderTicket(String mensaje) {
        Message msg = new Message(usuario.getID_User(),
                usuario.getID_User() == treeTableViewMensajes.getSelectionModel().getSelectedItem().getValue().getSenderID().get() ?
                        treeTableViewMensajes.getSelectionModel().getSelectedItem().getValue().getReceiverID().get() :
                        treeTableViewMensajes.getSelectionModel().getSelectedItem().getValue().getSenderID().get(),
                treeTableViewMensajes.getSelectionModel().getSelectedItem().getValue().getSubject().get(),
                mensaje,
                treeTableViewMensajes.getSelectionModel().getSelectedItem().getValue().getIdTicket().get(),
                false
        );
        // Actualizamos los mensajes
        List<Message> updatedMessages = modelo.getMessages();
        updatedMessages.add(msg);
        modelo.setMessages(updatedMessages);

        // Insertamos el mensaje en la BBDD
        conexionBBDD.insertarMensaje(msg.getIdTicket(), msg.getMessage(), msg.getSubject(), msg.getSenderID(), msg.getReceiverID());
        modelo.createAlert("Informacion", "Se ha enviado el mensaje correctamente");

        // Borramos la conversacion en caso de que hubiese una seleccionada para poder introducir la siguiente
        labelMessages.clear();
        vboxConversacionMensajes.getChildren().clear();
        changeTicketConversation(conexionBBDD.getMensajesDeTicket(treeTableViewMensajes.getSelectionModel().getSelectedItem().getValue().getIdTicket().get()));

    } // crearMensajeYResponderTicket()

    @FXML
    void responderTicketMensajes(ActionEvent event) throws IOException {
        // Cargamos 2nda escena
        FXMLLoader loaderResponderTicket = new FXMLLoader(getClass().getResource("/application/vistas/vistaEnviarMensaje.fxml"));
        Parent rootResponderTicket = loaderResponderTicket.load();

        // Cojemos el controlador de la 2nd escena
        controladorResponderTicket controladorCU = loaderResponderTicket.getController();
        controladorCU.initModelo(modelo, cp);

        // Display stage
        Stage stage = new Stage();
        stage.setScene(new Scene(rootResponderTicket));
        stage.show();
    } // responderTicketMensajes()

    // -------------------- Fin metodos tab Mensajes --------------------









    // -------------------- Metodos tab Registros --------------------

    @FXML
    void mostrarSensoresDia(ActionEvent event) {
        if (treeTableViewRegistros.getSelectionModel().getSelectedItem() != null) {
            llenarGraficasSensores();
            dumpRegistrosSensores();
        }
        else
            modelo.createAlert("Cuidado", "Primero debe escojer un usuario");
    } // mostrarSensoresDia()

    private void detailLabel(Label label, Font font, int prefWidth, Insets insets) {
        label.setFont(font);
        label.setPrefWidth(prefWidth);
        label.setWrapText(true);
        label.setPadding(insets);
    } // detialLabel()

    public void mostrarAlertasHashMap(HashMap<String, Vector<TextFlow>> hashMap) {
        for (Map.Entry<String, Vector<TextFlow>> entry : hashMap.entrySet()) {
            Label dateInfo = new Label("-------------- " + entry.getKey() + " --------------");
            detailLabel(dateInfo, Font.font("Century Gothic", FontWeight.BOLD, 17), 595, new Insets(0, 0, 0, 10));
            vboxRegistros_apane.getChildren().add(dateInfo);
            for (TextFlow registro : entry.getValue())
                vboxRegistros_apane.getChildren().add(registro);

            vboxRegistros_apane.getChildren().add(new Label(" "));
        }
    } // mostrarAlertasHashMap()

    private void dumpRegistrosSensores() {
        // Limpiamos los registros
        vboxRegistros_apane.getChildren().clear();

        // Creamos el hash map para las alertas
        LinkedHashMap<String, Vector<TextFlow>> alertasSensores = new LinkedHashMap<>();

        // Lo poblamos con datos
        conexionBBDD.recogerAlertas(alertasSensores, treeTableViewRegistros.getSelectionModel().getSelectedItem().getValue().getID_User().get(),
                                    formatter.format(calendarioSensores.getValue().minusWeeks(2).atStartOfDay()),
                                    formatter.format(calendarioSensores.getValue().atTime(23, 59, 59)) );

        mostrarAlertasHashMap(alertasSensores);



    } // dumpRegistrosSensores()

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void poblarGraficaMagnetico(String sentenciaDiscreto) {
        XYChart.Series series = new XYChart.Series();
        sentenciaDiscreto += " AND sensores_discretos.Reading = 1"; // Si lo comentamos tmbn añadimos los 0s a la grafica

        Vector<sensor> datosMagnetico = conexionBBDD.leerDatosSensor(treeTableViewRegistros.getSelectionModel().getSelectedItem().getValue().getID_User().get(),
                "Magnetico", formatter.format(calendarioSensores.getValue().atStartOfDay()),
                formatter.format(calendarioSensores.getValue().atTime(23, 59, 59)), sentenciaDiscreto);
        for (sensor sc : datosMagnetico) {
            Timestamp timestamp = new Timestamp(sc.getDate_Time_Activation().getTime());
            series.getData().add(new XYChart.Data(timestamp.toLocalDateTime().getHour()+"",sc.getReading()));
        }

        graficaMagnetico.getData().add(series);
    } // poblarGraficaMagnetico()

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void poblarGraficaGas(StackedBarChart<Double, Double> grafica, String sentencia,
                                      String tipoSensor) {
        // Gas
        XYChart.Series seriesGas = new XYChart.Series();


        for (sensor sc : conexionBBDD.leerDatosSensor(treeTableViewRegistros.getSelectionModel().getSelectedItem().getValue().getID_User().get(),
                         tipoSensor, formatter.format(calendarioSensores.getValue().atStartOfDay()),
                         formatter.format(calendarioSensores.getValue().atTime(23, 59, 59)), sentencia) ) {
            Timestamp timestamp = new Timestamp(sc.getDate_Time_Activation().getTime());
            seriesGas.getData().add(new XYChart.Data(timestamp.toLocalDateTime().getHour()+"", sc.getReading()));
        }

        grafica.getData().add(seriesGas);
    } // poblarGraficaGas()

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void poblarGraficaTemperatura(String sentenciaContinuo) {
        // Temperatura
        XYChart.Series seriesTemperatura = new XYChart.Series();

        for (sensor sc : conexionBBDD.leerDatosSensor(treeTableViewRegistros.getSelectionModel().getSelectedItem().getValue().getID_User().get(),
                "Temperatura", formatter.format(calendarioSensores.getValue().atStartOfDay()),
                formatter.format(calendarioSensores.getValue().atTime(23, 59, 59)), sentenciaContinuo)  ) {
            Timestamp timestamp = new Timestamp(sc.getDate_Time_Activation().getTime());
            seriesTemperatura.getData().add(new XYChart.Data(timestamp.toLocalDateTime().getHour()+"", sc.getReading()));
        }
        graficaTemperatura.getData().addAll(seriesTemperatura);
    } // poblarGraficaTemperatura()

    public void poblarGraficaPresion(String sentenciaDiscreto) {
        Vector<sensor> tuplasSinFiltrar = conexionBBDD.leerDatosSensor(treeTableViewRegistros.getSelectionModel().getSelectedItem().getValue().getID_User().get(),
                "Presion", formatter.format(calendarioSensores.getValue().atStartOfDay()),
                formatter.format(calendarioSensores.getValue().atTime(23, 59, 59)), sentenciaDiscreto);

        long totalMinutes = 0;
        long totalHours = 0;
        int i = 1;
        int end = 0;
        if (tuplasSinFiltrar.size() % 2 != 0) {
            if (tuplasSinFiltrar.get(0).getReading() == 0) {
                // de 00:00 a tuplasSinFiltar()
                Date d = Date.from(calendarioSensores.getValue().atStartOfDay().toInstant(ZoneOffset.UTC));
                Period p = new org.joda.time.Period(tuplasSinFiltrar.get(0).getDate_Time_Activation().getTime(), d.getTime());

                totalMinutes += p.getMinutes();
                totalHours += p.getHours();
                i = 2;
            } else {
                // del ultimo a 24:00
                Date d = Date.from(calendarioSensores.getValue().atTime(23, 59, 59).toInstant(ZoneOffset.UTC));
                Period p = new org.joda.time.Period(tuplasSinFiltrar.lastElement().getDate_Time_Activation().getTime(), d.getTime());

                totalMinutes += p.getMinutes();
                totalHours += p.getHours();
                end = 1;
            }
        }


        for ( ; i < tuplasSinFiltrar.size() - end; i += 2) {
            Period p = new org.joda.time.Period(tuplasSinFiltrar.get(i - 1).getDate_Time_Activation().getTime(), tuplasSinFiltrar.get(i).getDate_Time_Activation().getTime());
            totalMinutes += p.getMinutes();
            totalHours += p.getHours();
        }

        long totalTime = (totalHours*60) + totalMinutes;
        detalles.add(new PieChart.Data("Durmiendo", Duration.ofMinutes(totalTime).toMinutes()));
        detalles.add(new PieChart.Data("Despierto", 1440 - Duration.ofMinutes(totalTime).toMinutes()));
        graficaPresion.setData(detalles);
        horasDurmiendo.setText(LocalTime.MIN.plus(Duration.ofMinutes(totalTime)).toString());
        // horasDurmiendo.setText(totalHours + ":" + totalMinutes);
    } // poblarGraficaPresion()

    public void llenarGraficasSensores() {
        //Limpiamos todas las gráficas
        graficaTemperatura.getData().clear();
        graficaMagnetico.getData().clear();
        graficaGas.getData().clear();
        graficaPresion.getData().clear();
        detalles.clear();

        String sentenciaContinuo = "SELECT ID_Sensores_Continuos, AVG(sensores_continuos.Reading) AS Reading, Date_Time_Activation\n" +
                "FROM sensores_continuos\n" +
                "INNER JOIN sensores ON sensores_continuos.Sensores_ID1 = sensores.ID_Sensor\n" +
                "INNER JOIN users ON sensores.Users_ID1 = users.ID_User\n" +
                "WHERE users.ID_User = ? AND sensores.`Type` = ? AND sensores_continuos.Date_Time_Activation BETWEEN ? AND ?\n" +
                "GROUP BY HOUR(Date_Time_Activation)";

        String sentenciaDiscreto = "SELECT sensores_discretos.*\n" +
                "FROM sensores_discretos\n" +
                "INNER JOIN sensores ON sensores_discretos.Sensores_ID2 = sensores.ID_Sensor\n" +
                "INNER JOIN users ON sensores.Users_ID1 = users.ID_User\n" +
                "WHERE users.ID_User = ? AND sensores.`Type` = ? AND sensores_discretos.Date_Time_Activation BETWEEN ? AND ?";

        if (treeTableViewRegistros.getSelectionModel().getSelectedItem() != null) {

            poblarGraficaTemperatura(sentenciaContinuo);

            poblarGraficaGas(graficaGas, sentenciaContinuo, "Gas");

            poblarGraficaMagnetico(sentenciaDiscreto);

            poblarGraficaPresion(sentenciaDiscreto);
            
            graficaTemperatura.setLegendVisible(false);
            graficaPresion.setLegendVisible(false);
            graficaGas.setLegendVisible(false);
            graficaMagnetico.setLegendVisible(false);
        }
    } // llenarGraficaSensores()

    @FXML
    void mostrarDatosSensoresPacientes(MouseEvent event) {
        if (calendarioSensores.getValue() != null) {
            llenarGraficasSensores();
            dumpRegistrosSensores();
        }
    } // mostrarDatosSensoresPacientes()

    // -------------------- Fin metodos tab Registros --------------------









    // -------------------- Metodos tab Localizacion --------------------
    
    public void initialize(URL url, ResourceBundle rb) {
        mapView.setKey("AIzaSyABUQnPXeldroN__fhm1LDiZh5sUtkSMBM"); // No usar
        mapView.addMapInializedListener(this);
        address.bind(addressTextField.textProperty());
    }
    
    public void mapInitialized() {
        MapOptions mapOptions = new MapOptions();

        mapOptions.center(new LatLong(40.371830555556, -3.9189527777778))
                .mapType(MapTypeIdEnum.ROADMAP)
                .overviewMapControl(false)
                .panControl(false)
                .rotateControl(false)
                .scaleControl(false)
                .streetViewControl(false)
                .zoomControl(false)
                .zoom(16);

        map = mapView.createMap(mapOptions);
        System.out.println(LocalTime.now()+": GoogleMaps cargado");

    }
    
    @FXML
    public void addressTextFieldAction(ActionEvent event) {
    	map.clearMarkers();
        geocodingService.geocode(address.get(), (GeocodingResult[] results, GeocoderStatus status) -> {
            LatLong latLong;

            // No hubo resultados
            if (status == GeocoderStatus.ZERO_RESULTS) modelo.createAlert("Error", "No se encontraron direcciones coincidentes");

            if (results.length > 1) modelo.createAlert("Informacion", "Multiples resultados encontrados, mostrando el primero.");

            latLong = new LatLong(results[0].getGeometry().getLocation().getLatitude(), results[0].getGeometry().getLocation().getLongitude());
            map.setCenter(latLong);
            
            // Añadir un Marker al mapa
            MarkerOptions markerOptions = new MarkerOptions();

            markerOptions.position(new LatLong(40.371830555556, -3.9189527777778))
                    .visible(Boolean.TRUE)
                    .title("My Marker");

            Marker marker = new Marker(markerOptions);
            map.addMarker(marker);
            
        });
    }

    @FXML
    void verUbicacionCasa(ActionEvent event) {
        geocodingService = new GeocodingService();
    	map.clearMarkers();
    	if (treeTableViewLocalizacion.getSelectionModel().getSelectedItem() != null) {
            geocodingService.geocode(treeTableViewLocalizacion.getSelectionModel().getSelectedItem().getValue().getAdress().get(), (GeocodingResult[] results, GeocoderStatus status) -> {
                LatLong latLong;
                // No hubo resultados
                if (status == GeocoderStatus.ZERO_RESULTS)
                	modelo.createAlert("Error", "No se encontraron direcciones coincidentes");

                if (results.length > 1) 
                	modelo.createAlert("Informacion", "Multiples resultados encontrados, mostrando el primero.");

                latLong = new LatLong(results[0].getGeometry().getLocation().getLatitude(), results[0].getGeometry().getLocation().getLongitude());
                map.setCenter(latLong);
                
                // Añadir un Marker al mapa de la casa
                MarkerOptions markerOptions = new MarkerOptions();

                markerOptions.position(latLong)
                        .visible(Boolean.TRUE)
                        .title("Casa de "+ treeTableViewLocalizacion.getSelectionModel().getSelectedItem().getValue().getName().get()+" "+treeTableViewLocalizacion.getSelectionModel().getSelectedItem().getValue().getSurname().get());
                Marker marker = new Marker(markerOptions);
         
                map.addMarker(marker);
            });
		} else {
			modelo.createAlert("Selección de usuario", "Debes seleccionar un usuario antes de poder ver la ubicación");
		}
    }
    
    @FXML
    void ubicacionPaciente(ActionEvent event) {
        geocodingService = new GeocodingService();
    	map.clearMarkers();
    	if (treeTableViewLocalizacion.getSelectionModel().getSelectedItem() != null) {
        	LatLong latlong = null;
            String sentenciaGPS = "SELECT sensor_GPS.*\n" +
                    "FROM sensor_GPS\n" +
                    "INNER JOIN sensores ON sensor_GPS.Sensores_ID3 = sensores.ID_Sensor\n" +
                    "INNER JOIN users ON sensores.Users_ID1 = users.ID_User\n" +
                    "WHERE users.ID_User = ? AND sensores.`Type` = ?";
            
            for (sensor sg : conexionBBDD.leerDatosSensorGPS(treeTableViewLocalizacion.getSelectionModel().getSelectedItem().getValue().getID_User().get(),
                                                    "GPS" , sentenciaGPS)) {
            	latlong = new LatLong (sg.getLatitude(), sg.getLongitude());
            }
            map.setCenter(latlong);
            // Añadir un Marker al mapa de la casa
            MarkerOptions markerOptions = new MarkerOptions();
            
            markerOptions.position(latlong)
                    .visible(Boolean.TRUE)
                    .title("Ubicacion en tiempo real de "+ treeTableViewLocalizacion.getSelectionModel().getSelectedItem().getValue().getName().get()+" "+treeTableViewLocalizacion.getSelectionModel().getSelectedItem().getValue().getSurname().get());
            		
            Marker marker = new Marker(markerOptions);
            marker.setTitle("Ubicacion");
     
            map.addMarker(marker);
    	} else {
    		modelo.createAlert("Selección de usuario", "Debes seleccionar un usuario antes de poder ver la ubicación");
    	}
    }

//    @FXML
//    void mostrarDatosMapaPacientes(MouseEvent event) throws ParseException {
//        calendarioSensores.setValue(LocalDate.now()); // Asignamos la fecha actual al seleccionar un usuario
//    }
    
    // -------------------- Fin metodos tab Localizacion --------------------

} // controladorVistaGeneral()

