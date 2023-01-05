package com.test.book_test;

import static io.restassured.RestAssured.given;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOf;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import io.qameta.allure.Step;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

public class BookTest {

    WebDriver driver;
    WebDriverWait wait;

    String usernameData = "{\n"
            + "  \"userName\": \"Test\",\n"
            + "  \"password\": \"Qaz1234!\"\n"
            + "}";

    public void setUp() {
        System.setProperty("webdriver.chrome.driver","chromedriver.exe");
        driver = new ChromeDriver();
        wait = new WebDriverWait(driver, 15);
        driver.manage().window().maximize();
        driver.get("https://demoqa.com/books");
        wait.until(visibilityOf(
                driver.findElement(By.xpath("//div[@class = 'books-wrapper']"))));
    }

    @Test
    public void addBook() {
        setUp();
        login();
        String bookName = addBookToCollection();
        checkBookIsAdded(bookName);
        //deleting book in the end to avoid errors in other tests
        deleteBookAndCheck(bookName);
    }

    @Test
    public void deleteBook() {
        setUp();
        login();
        String bookName = addBookToCollection();
        deleteBookAndCheck(bookName);
    }

    @Test
    public void addBookAPI() {
        String token = generateToken();
        String userId = apiLogin();
        deleteBooksInCollection(token, userId);
        addBookToCollection(token, userId);

    }

    @AfterMethod(alwaysRun = true)
    public void closeBrowser(){
        if (driver != null)
            driver.quit();
    }

    public WebElement findElement(String xpath) {
        return driver.findElement(By.xpath(xpath));
    }

    @Step("Login process")
    private void login() {
        findElement("//button[@id = 'login']").click();
        wait.until(visibilityOf(findElement("//form[@id = 'userForm']")));
        findElement("//input[@id = 'userName']").sendKeys("Test");
        findElement("//input[@id = 'password']").sendKeys("Qaz1234!");
        findElement("//button[@id = 'login']").click();
        customWait(3);
        wait.until(visibilityOf(findElement("//div[@class = 'ReactTable -striped -highlight']")));
    }

    @Step("Adding book to collection")
    private String addBookToCollection() {
        findElement("//div[@class = 'action-buttons'][1]").click();
        wait.until(visibilityOf(findElement("//button[contains(text(), 'Add To Your Collection') ]")));
        String bookName = findElement("//div[@id= 'title-wrapper' ]//label[@id = 'userName-value']").getText();
        findElement("//button[contains(text(), 'Add To Your Collection') ]").click();
        customWait(3);
        closeAlertIfPresent();
        return bookName;
    }

    @Step("Cheking that book is added to collection")
    private void checkBookIsAdded(String bookName) {
        driver.get("https://demoqa.com/profile");
        customWait(3);
        String bookXpath = "//a[contains(text(), '" + bookName + "')]";
        int bookTableSize = driver.findElements(By.xpath(bookXpath)).size();
        assertEquals(bookTableSize, 1, "Book is not added to the collection");
        assertTrue(findElement(bookXpath).isDisplayed(), "Book is not added to the collection");
    }

    @Step("Cheking that book is deleted from collection")
    private void deleteBookAndCheck(String bookName) {
        driver.get("https://demoqa.com/profile");
        customWait(3);
        String bookXpath = "//a[contains(text(), '" + bookName + "')]";
        String deleteBookXpath = bookXpath +
                "//ancestor::div[@class = 'rt-tr-group']//span[@id = 'delete-record-undefined']";
        findElement(deleteBookXpath).click();
        findElement("//button[@id = 'closeSmallModal-ok']").click();
        customWait(3);
        closeAlertIfPresent();
        int bookTableSize = driver.findElements(By.xpath(bookXpath)).size();
        assertEquals(bookTableSize, 0, "Book table is not empty");
    }

    @Step ("Generating token")
    private String generateToken() {
        return given().contentType("application/json")
                .body(usernameData)
                .when()
                .post("https://demoqa.com/Account/v1/GenerateToken")
                .then()
                .assertThat().statusCode(200)
                .extract().path("token");
    }

    @Step
    private void deleteBooksInCollection(String token, String userId) {
        given()
                .header("Authorization", "Bearer " + token)
                .queryParam("UserId", userId)
                .when()
                .delete("https://demoqa.com/BookStore/v1/Books")
                .then()
                .assertThat().statusCode(204);
    }

    @Step
    private void addBookToCollection(String token, String userId) {
        given().contentType("application/json")
                .header("Authorization", "Bearer " + token)
                .body("{\n"
                        + "  \"userId\": \""+ userId + "\",\n"
                        + "  \"collectionOfIsbns\": [\n"
                        + "    {\n"
                        + "      \"isbn\": \"9781449325862\"\n"
                        + "    }\n"
                        + "  ]\n"
                        + "}")
                .when()
                .post("https://demoqa.com/BookStore/v1/Books")
                .then()
                .assertThat().statusCode(201);
    }

    @Step
    private String apiLogin() {
        return given().contentType("application/json")
                .body(usernameData)
                .when()
                .post("https://demoqa.com/Account/v1/Login")
                .then()
                .assertThat().statusCode(200)
                .extract().path("userId");
    }

    private void closeAlertIfPresent() {
        try {
            Alert alert = driver.switchTo().alert();
            alert.accept();
        }
        catch (NoAlertPresentException e) {}
    }

    private static void customWait(int seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
