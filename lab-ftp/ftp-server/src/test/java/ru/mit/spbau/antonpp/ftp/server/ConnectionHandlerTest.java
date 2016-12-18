package ru.mit.spbau.antonpp.ftp.server;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import ru.mit.spbau.antonpp.ftp.protocol.RequestCode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.mockito.Mockito.when;

/**
 * @author antonpp
 * @since 11/12/2016
 */
@RunWith(MockitoJUnitRunner.class)
public class ConnectionHandlerTest {

    private static final String TEST_DIR = ConnectionHandlerTest.class.getClassLoader()
            .getResource("test_dir").getPath();
    @Mock
    private Socket clientSocket;
    private ConnectionHandler handler;
    private List<Path> testPaths;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        handler = new ConnectionHandler(clientSocket);

        final Path testPath = Paths.get(TEST_DIR);
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(testPath)) {
            testPaths = StreamSupport.stream(ds.spliterator(), false).sorted()
                    .collect(Collectors.toList());
        }
    }

    @Test
    public void testGetInvalidFile() throws IOException {
        final Path test = Paths.get("Not/existing/path/ever/never");
        final ByteArrayOutputStream expectedBytes = new ByteArrayOutputStream();
        try (final DataOutputStream expectedOS = new DataOutputStream(expectedBytes)) {
            expectedOS.writeLong(0);
        }
        final byte[] expected = expectedBytes.toByteArray();
        final ByteArrayOutputStream requestBytes = new ByteArrayOutputStream();


        try (final DataOutputStream dos = new DataOutputStream(requestBytes)) {
            dos.writeInt(RequestCode.RQ_GET);
            dos.writeUTF(test.toString());
            dos.writeInt(RequestCode.RQ_DISCONNECT);
        }

        final ByteArrayInputStream requestIS = new ByteArrayInputStream(requestBytes.toByteArray());
        final ByteArrayOutputStream responseOS = new ByteArrayOutputStream();

        when(clientSocket.getInputStream()).thenReturn(requestIS);
        when(clientSocket.getOutputStream()).thenReturn(responseOS);

        handler.run();

        Assert.assertArrayEquals(expected, responseOS.toByteArray());
    }

    @Test
    public void testGetDir() throws IOException {
        final Path test = testPaths.stream().filter(Files::isDirectory).findAny()
                .orElseThrow(() -> new IllegalStateException("test dir was not found"));
        final ByteArrayOutputStream expectedBytes = new ByteArrayOutputStream();
        try (final DataOutputStream expectedOS = new DataOutputStream(expectedBytes)) {
            expectedOS.writeLong(0);
        }
        final byte[] expected = expectedBytes.toByteArray();
        final ByteArrayOutputStream requestBytes = new ByteArrayOutputStream();


        try (final DataOutputStream dos = new DataOutputStream(requestBytes)) {
            dos.writeInt(RequestCode.RQ_GET);
            dos.writeUTF(test.toString());
            dos.writeInt(RequestCode.RQ_DISCONNECT);
        }

        final ByteArrayInputStream requestIS = new ByteArrayInputStream(requestBytes.toByteArray());
        final ByteArrayOutputStream responseOS = new ByteArrayOutputStream();

        when(clientSocket.getInputStream()).thenReturn(requestIS);
        when(clientSocket.getOutputStream()).thenReturn(responseOS);

        handler.run();

        Assert.assertArrayEquals(expected, responseOS.toByteArray());
    }

    @Test
    public void testGetFile() throws IOException {
        final List<Path> testFiles = testPaths.stream()
                .filter(Files::isRegularFile).collect(Collectors.toList());
        final ByteArrayOutputStream expectedBytes = new ByteArrayOutputStream();
        for (Path test : testFiles) {
            try (final DataOutputStream expectedOS = new DataOutputStream(expectedBytes)) {
                expectedOS.writeLong(Files.size(test));
                expectedOS.write(Files.readAllBytes(test));
            }
        }
        final byte[] expected = expectedBytes.toByteArray();
        final ByteArrayOutputStream requestBytes = new ByteArrayOutputStream();


        try (final DataOutputStream dos = new DataOutputStream(requestBytes)) {
            for (Path test : testFiles) {
                dos.writeInt(RequestCode.RQ_GET);
                dos.writeUTF(test.toString());
            }
            dos.writeInt(RequestCode.RQ_DISCONNECT);
        }

        final ByteArrayInputStream requestIS = new ByteArrayInputStream(requestBytes.toByteArray());
        final ByteArrayOutputStream responseOS = new ByteArrayOutputStream();

        when(clientSocket.getInputStream()).thenReturn(requestIS);
        when(clientSocket.getOutputStream()).thenReturn(responseOS);

        handler.run();

        Assert.assertArrayEquals(expected, responseOS.toByteArray());
    }

    @Test
    public void testList() throws IOException {
        final ByteArrayOutputStream expectedBytes = new ByteArrayOutputStream();
        try (final DataOutputStream expectedOS = new DataOutputStream(expectedBytes)) {
            expectedOS.writeInt(testPaths.size());
            for (Path p : testPaths) {
                expectedOS.writeUTF(p.getFileName().toString());
                expectedOS.writeBoolean(Files.isDirectory(p));
            }
        }
        final byte[] expected = expectedBytes.toByteArray();

        final ByteArrayOutputStream requestBytes = new ByteArrayOutputStream();
        try (final DataOutputStream dos = new DataOutputStream(requestBytes)) {
            dos.writeInt(RequestCode.RQ_LIST);
            dos.writeUTF(TEST_DIR);
            dos.writeInt(RequestCode.RQ_DISCONNECT);
        }
        final ByteArrayInputStream requestIS = new ByteArrayInputStream(requestBytes.toByteArray());
        final ByteArrayOutputStream responseOS = new ByteArrayOutputStream();

        when(clientSocket.getInputStream()).thenReturn(requestIS);
        when(clientSocket.getOutputStream()).thenReturn(responseOS);

        handler.run();

        Assert.assertArrayEquals(expected, responseOS.toByteArray());


    }

}