package src.app;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

/**
 * @Author Adam Abbott
 */

public class AmmoPriceChecker {

    /**
     * 
     * @param url - The URL given from the main method where it can find the proper
     *            fields (ga-cpr) or cents per round.
     * @return total/count - Returns the average price per round.
     */
    private static double getAveragePrice(String url) {
        System.setProperty("webdriver.chrome.driver", "chromedriver");
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");

        WebDriver driver = null;
        try {
            driver = new ChromeDriver(options);
            driver.get(url);

            WebDriverWait wait = new WebDriverWait(driver, 10);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".ga-cpr")));

            List<WebElement> priceElements = driver.findElements(By.cssSelector(".ga-cpr"));

            double total = 0;
            int count = 0;

            for (WebElement element : priceElements) {
                String priceText = element.getText().replaceAll("[^\\d.]", "");
                try {
                    double price = Double.parseDouble(priceText);
                    total += price;
                    count++;
                } catch (NumberFormatException e) {
                    // Silently skip unparseable prices
                }
            }

            if (count > 0) {
                return total / count;
            } else {
                return 0.0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    public static void readCSV(String csvFile) throws CsvValidationException {
        try (CSVReader reader = new CSVReader(new FileReader(csvFile))) {
            String[] nextLine;
            boolean headerSkipped = false;
            while ((nextLine = reader.readNext()) != null) {
                if (!headerSkipped) {
                    headerSkipped = true;
                    continue; // Skip the header row
                }
                if (nextLine.length < 3) {
                    System.out.println("Skipping invalid line: " + String.join(",", nextLine));
                    continue;
                }
                String caliber = nextLine[0].trim();
                String date = nextLine[1].trim();
                double cpr;
                try {
                    cpr = Double.parseDouble(nextLine[2].trim());
                } catch (NumberFormatException e) {
                    System.out.println("Invalid CPR value in line: " + String.join(",", nextLine));
                    continue;
                }
                dataSet(caliber, cpr, date);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException, CsvValidationException {

        String[] urls = { "https://ammoseek.com/ammo/9mm-luger?sh=lowest&ca=brass",
                "https://ammoseek.com/ammo/223-remington/-rifle-55grains?ca=brass&sh=lowest",
                "https://ammoseek.com/ammo/12-gauge?sl=2%203%2F4&sh=lowest" };
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yy");
        String d = dateFormat.format(new Date());

        String csvFile = "Test.csv";

        for (int i = 0; i < urls.length; i++) {

            if (urls[i].contains("9mm-luger")) {
                BufferedWriter out = new BufferedWriter(new FileWriter(csvFile, true));
                out.newLine();
                out.write("9mm," + d + "," + String.format("%.2f", getAveragePrice(urls[i])));
                out.flush();
                out.close();
            } else if (urls[i].contains("223-remington")) {
                BufferedWriter out = new BufferedWriter(new FileWriter(csvFile, true));
                out.newLine();
                out.write(".223rem," + d + "," + String.format("%.2f", getAveragePrice(urls[i])));
                out.flush();
                out.close();
            } else if (urls[i].contains("12-gauge")) {
                BufferedWriter out = new BufferedWriter(new FileWriter(csvFile, true));
                out.newLine();
                out.write("12-gauge," + d + "," + String.format("%.2f", getAveragePrice(urls[i])));
                out.flush();
                out.close();
            } else {
                BufferedWriter out = new BufferedWriter(new FileWriter(csvFile, true));
                out.newLine();
                out.write("0," + "0," + "0," + "0");
                out.flush();
                out.close();
            }
        }

        readCSV(csvFile);
        createCharts();
    }
}