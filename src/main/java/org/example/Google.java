package org.example;

import com.google.api.client.auth.oauth2.Credential ;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp ;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver ;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow ;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets ;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport ;
import com.google.api.client.http.javanet.NetHttpTransport ;
import com.google.api.client.json.JsonFactory ;
import com.google.api.client.json.gson.GsonFactory ;
import com.google.api.client.util.DateTime ;
import com.google.api.client.util.store.FileDataStoreFactory ;
import com.google.api.services.calendar.Calendar ;
import com.google.api.services.calendar.CalendarScopes ;
import com.google.api.services.calendar.model.Event ;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.Events ;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.FileList;

import java.io.*;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class Google {
    private static final String applicationName = "IpserLab/Bandung" ;
    private static final JsonFactory jsonFactory = GsonFactory.getDefaultInstance() ;
    private static final List<String> scope = Collections.singletonList(CalendarScopes.CALENDAR_READONLY) ;
    private static final String propertiesFilename = "Properties.txt" ;
    private static String apiKeyGoogle ;
    private static String tokenPathname ;
    private static final String credentialsPathname = "credentials.json";

    private Drive driveService;
    private Calendar calendarService;

    public Google(Drive driveService, Calendar calendarService) {
        this.driveService = driveService;
        this.calendarService = calendarService;
    }

    private static Credential getCredentials (final NetHttpTransport HTTP_TRANSPORT)
            throws IOException
    {


        InputStream in = new FileInputStream(credentialsPathname);
        if (in==null)
            throw new FileNotFoundException("File not found: "+credentialsPathname) ;
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(jsonFactory,new InputStreamReader(in));
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT,jsonFactory,clientSecrets,scope)
                        .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(tokenPathname)))
                        .setAccessType("offline")
                        .build() ;
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build() ;
        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user") ;
        return credential ;
    }
    public boolean getCalendarUser(String eventId, String email) throws IOException {
        Event event = calendarService.events().get("primary", eventId).execute();
        List<EventAttendee> attendees = event.getAttendees();
        if (attendees != null) {
            for (EventAttendee attendee : attendees) {
                if (attendee.getEmail().equals(email)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void addCalendarUser (String event, String email)
    {
        //add the user to the event whether or not he is in there already
    }
    public void removeCalendarUser (String event, String email)
    {
        //remove the user from the event whether or not he is in there already
    }
    public static String[] getCalendarEvents(NetHttpTransport httpTransport)
            throws IOException
    {
        Calendar calendar =
                new Calendar.Builder(httpTransport,jsonFactory,getCredentials(httpTransport))
                        .setApplicationName(applicationName)
                        .build() ;
        DateTime now = new DateTime(System.currentTimeMillis()) ;
        Events events =
                calendar.events().list("primary")
                        .setMaxResults(10)
                        .setTimeMin(now)
                        .setOrderBy("startTime")
                        .setSingleEvents(true)
                        .execute() ;
        List<Event> eventList = events.getItems() ;
        if (eventList.isEmpty())
            System.out.println("No upcoming events found.") ;
        else
        {
            System.out.println("Upcoming events") ;
            for (Event event : eventList)
            {
                DateTime start = event.getStart().getDateTime() ;
                if (start== null)
                    start = event.getStart().getDate() ;
                System.out.printf("%s (%s)\n", event.getSummary(), start) ;
            }
        }
        return null ;
    }



    public static List<Event> getCalendarEventsByDateAndName(NetHttpTransport httpTransport, DateTime startDate, String eventName) throws IOException {
        Calendar calendar =
                new Calendar.Builder(httpTransport, jsonFactory, getCredentials(httpTransport))
                        .setApplicationName(applicationName)
                        .build();
        Events events =
                calendar.events().list("primary")
                        .setTimeMin(startDate)
                        .setOrderBy("startTime")
                        .setSingleEvents(true)
                        .execute();
        List<Event> eventList = events.getItems();
        List<Event> filteredEvents = new ArrayList<>();
        if (eventList.isEmpty()) {
            System.out.println("No upcoming events found.");
        } else {
            for (Event event : eventList) {
                if (event.getSummary().equalsIgnoreCase(eventName)) {
                    filteredEvents.add(event);
                }
            }
        }
        return filteredEvents;
    }














    public boolean getFolderUser (String folder, String email)
    {
        //return true if user is in the folder and false if not
        return false ;
    }
    public void addFolderUser (String folder, String email)
    {
        //add the user to the folder whether or not he is in there already
    }
    public void removeFolderUser (String folder, String email)
    {
        //remove the user from the folder whether or not he is in there already
    }

    public String[] getFolderFolders(String folderName) throws IOException {
        // Find the folder by name
        String folderId = null;
        String query = "name = '" + folderName + "' and mimeType = 'application/vnd.google-apps.folder' and trashed = false";
        FileList result = driveService.files().list()
                .setQ(query)
                .setFields("files(id, name)")
                .execute();

        if (result.getFiles().isEmpty()) {
            System.out.println("No folder found with the name: " + folderName);
            return new String[0];
        } else {
            folderId = result.getFiles().get(0).getId();
        }

        // List subfolders within the found folder
        List<String> folderNames = new ArrayList<>();
        query = "'" + folderId + "' in parents and mimeType = 'application/vnd.google-apps.folder' and trashed = false";
        result = driveService.files().list()
                .setQ(query)
                .setFields("files(id, name)")
                .execute();

        for (com.google.api.services.drive.model.File file : result.getFiles()) {
            folderNames.add(file.getName());
        }

        return folderNames.toArray(new String[0]);
    }

    public static void main (String... args)
            throws IOException, GeneralSecurityException
    {
        File credentialsFile = new File(propertiesFilename) ;
        Properties properties = Utility.loadProperties(credentialsFile) ;
        Google.apiKeyGoogle = properties.getProperty("GoogleApiKey") ;
        Google.tokenPathname = properties.getProperty("token") ;
//        Google.credentialsPathname = properties.getProperty("credentials") ;
        final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport() ;








        // Initialize the Drive service
        Drive driveService = new Drive.Builder(httpTransport, jsonFactory, getCredentials(httpTransport))
                .setApplicationName(applicationName)
                .build();
        Calendar calendarService = new Calendar.Builder(httpTransport, jsonFactory, getCredentials(httpTransport))
                .setApplicationName(applicationName)
                .build();
        Google google = new Google(driveService,calendarService);


        //        //get events
//        String[] events = getCalendarEvents(httpTransport);
//
//        assert events != null;
//        for (String event : events)
//            System.out.println(event) ;

//        // Example usage of getFolderFolders
//        String folderName = "postgres";
//        String[] folders = google.getFolderFolders(folderName);
//        for (String folder : folders) {
//            System.out.println(folder);
//        }





        // Example usage of getCalendarEventsByDateAndName
        DateTime startDate = new DateTime(System.currentTimeMillis());
        String eventName = "PfH: TF demo and next planning";
        List<Event> events = getCalendarEventsByDateAndName(httpTransport, startDate, eventName);
        for (Event event : events) {
            System.out.println("Event ID: " + event.getId() + ", Event Name: " + event.getSummary());
        }

        // Example usage of getCalendarUser
        String eventId = events.getFirst().getId();
        String email = "example@gmail.com";
        boolean isUserInEvent = google.getCalendarUser(eventId, email);
        System.out.println("Is user in event: " + isUserInEvent);

        // Existing code...
    }
}