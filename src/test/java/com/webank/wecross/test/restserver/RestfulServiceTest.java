package com.webank.wecross.test.restserver;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

// To run with: gradle test --tests RestfulServiceTest

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class RestfulServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void okTest() throws Exception {
        try {
            MvcResult rsp =
                    this.mockMvc
                            .perform(get("/test"))
                            .andDo(print())
                            .andExpect(status().isOk())
                            .andReturn();

            String result = rsp.getResponse().getContentAsString();
            System.out.println("####Respond: " + result);

            String expectRsp = "OK!";
            Assert.assertEquals(expectRsp, result);
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage(), false);
        }
    }

    @Test
    public void existTest() throws Exception {
        try {
            MvcResult rsp =
                    this.mockMvc
                            .perform(get("/test-network/test-stub/test-resource/exists"))
                            .andDo(print())
                            .andExpect(status().isOk())
                            .andReturn();

            String result = rsp.getResponse().getContentAsString();
            System.out.println("####Respond: " + result);

            String expectRsp =
                    "{\"version\":\"0.1\",\"result\":0,\"message\":null,\"data\":\"exists!\"}";
            Assert.assertEquals(expectRsp, result);
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage(), false);
        }
    }

    @Test
    public void listTest() throws Exception {
        try {
            String json =
                    "{\n"
                            + "\"version\":\"0.1\",\n"
                            + "\"path\":\"\",\n"
                            + "\"method\":\"list\",\n"
                            + "\"data\": {\n"
                            + "\"ignoreRemote\":true\n"
                            + "}\n"
                            + "}";

            MvcResult rsp =
                    this.mockMvc
                            .perform(
                                    post("/list").contentType(MediaType.APPLICATION_JSON).content(json))
                            .andDo(print())
                            .andExpect(status().isOk())
                            .andReturn();

            String result = rsp.getResponse().getContentAsString();
            System.out.println("####Respond: " + result);

            String expectRsp = "{\"version\":\"0.1\",\"result\":0,\"message\":null,\"data\":{\"errorCode\":0,\"errorMessage\":\"\",\"resources\":[{\"type\":\"";
            Assert.assertTrue(result.contains(expectRsp));
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage(), false);
        }

    }

    @Test
    public void callTest() throws Exception {
        try {
            String json =
                    "{\n"
                            + "\"version\":\"0.1\",\n"
                            + "\"path\":\"test-network.test-stub.test-resource/\",\n"
                            + "\"method\":\"call\",\n"
                            + "\"data\": {\n"
                            + "\"sig\":\"\",\n"
                            + "\"method\":\"get\",\n"
                            + "\"args\":[]\n"
                            + "}\n"
                            + "}";

            MvcResult rsp =
                    this.mockMvc
                            .perform(
                                    post("/test-network/test-stub/test-resource//call")
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(json))
                            .andDo(print())
                            .andExpect(status().isOk())
                            .andReturn();

            String result = rsp.getResponse().getContentAsString();
            System.out.println("####Respond: " + result);

            String expectRsp =
                    "{\"version\":\"0.1\",\"result\":0,\"message\":null,\"data\":{\"errorCode\":0,\"errorMessage\":\"Call test resource success\",\"hash\":\"010157f4\",\"result\":[{\"sig\":\"\",\"method\":\"get\",\"args\":[]}]}}";
            Assert.assertEquals(expectRsp, result);
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage(), false);
        }
    }

    @Test
    public void sendTransactionTest() throws Exception {
        try {
            String json =
                    "{\n"
                            + "\"version\":\"0.1\",\n"
                            + "\"path\":\"test-network.test-stub.test-resource\",\n"
                            + "\"method\":\"sendTransaction\",\n"
                            + "\"data\": {\n"
                            + "\"sig\":\"\",\n"
                            + "\"method\":\"set\",\n"
                            + "\"args\":[\"aaaaa\"]\n"
                            + "}\n"
                            + "}";

            MvcResult rsp =
                    this.mockMvc
                            .perform(
                                    post("/test-network/test-stub/test-resource/sendTransaction")
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(json))
                            .andDo(print())
                            .andExpect(status().isOk())
                            .andReturn();

            String result = rsp.getResponse().getContentAsString();
            System.out.println("####Respond: " + result);

            String expectRsp =
                    "{\"version\":\"0.1\",\"result\":0,\"message\":null,\"data\":{\"errorCode\":0,\"errorMessage\":\"sendTransaction test resource success\",\"hash\":\"010157f4\",\"result\":[{\"sig\":\"\",\"method\":\"set\",\"args\":[\"aaaaa\"]}]}}";
            Assert.assertEquals(expectRsp, result);
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage(), false);
        }
    }

    @Test
    public void exceptionTest() {
        try {
            String json =
                    "{\n"
                            + "\"version\":\"0.1\",\n"
                            + "\"path\":\"test-network.test-stub.test-resource\",\n"
                            + "\"method\":\"sendTransaction\",\n"
                            + "\"data\": {\n"
                            + "\"sig\":\"\",\n"
                            + "\"method\":\"set\",\n"
                            + "\"args\":[\"aaaaa\"]\n"
                            + "}\n"
                            + "}";

            MvcResult rsp =
                    this.mockMvc
                            .perform(
                                    post("/test-network/test-stub/test-resource/notExistMethod")
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(json))
                            .andDo(print())
                            .andExpect(status().isOk())
                            .andReturn();

            String result = rsp.getResponse().getContentAsString();
            System.out.println("####Respond: " + result);

            String expectRsp = "{\"version\":\"0.1\",\"result\":-1,\"message\":\"Unsupported method: notExistMethod\",\"data\":null}";
            Assert.assertTrue(result.contains(expectRsp));
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage(), false);
        }
    }

    @Before
    public void beforeTest() {
        System.out.println("------------------------Test begin------------------------");
    }

    @After
    public void afterTest() {
        System.out.println("-------------------------Test end-------------------------");
    }

    @BeforeClass
    public static void beforeClassTest() {
        System.out.println("beforeClassTest");
    }

    @AfterClass
    public static void afterClassTest() {
        System.out.println("afterClassTest");
    }
}
