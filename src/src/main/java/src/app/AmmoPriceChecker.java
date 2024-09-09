package src.app;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.*;
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.opencsv.*;
import com.opencsv.exceptions.CsvValidationException;
/**
 * @Author Adam Abbott
 */

public class AmmoPriceChecker {

    /**
     * 
     * @param url - The URL given from the main method where it can find the proper fields (ga-cpr) or cents per round. 
     * @return total/count - Returns the average price per round. 
     */
    private static double getAveragePrice(String url) {
        System.setProperty("webdriver.chrome.driver", "/home/adamabbott/Documents/VSCode/Project1/lib/chromedriver-linux64/chromedriver");
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");

        WebDriver driver = null;
        try {
            driver = new ChromeDriver(options);
            driver.get(url);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
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

    private static Map<String, DefaultCategoryDataset> datasets = new HashMap<>();

    public static void dataSet(String caliber, double cpr, String date) { 
        DefaultCategoryDataset dataset = datasets.computeIfAbsent(caliber, k -> new DefaultCategoryDataset());
        dataset.addValue(cpr, caliber, date);
    }

    public static void createCharts() {
        for (Map.Entry<String, DefaultCategoryDataset> entry : datasets.entrySet()) {
            String caliber = entry.getKey();
            DefaultCategoryDataset dataset = entry.getValue();

            JFreeChart chart = ChartFactory.createLineChart(
                caliber + " Ammo Price Tracker",
                "Date",
                "Cents Per Round",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
            );
            LineAndShapeRenderer renderer = new LineAndShapeRenderer();
            renderer.setDefaultShapesVisible(true);
            chart.getCategoryPlot().setRenderer(renderer);


            // Format Y-axis as currency
            NumberAxis yAxis = (NumberAxis) chart.getCategoryPlot().getRangeAxis();
            yAxis.setNumberFormatOverride(new DecimalFormat("0.00¢"));

            try {
                ChartUtils.saveChartAsPNG(new File("src/data/" + caliber + ".png"), chart, 800, 600);
                System.out.println("Chart created for " + caliber);
            } catch (Exception e) {
                System.err.println("Error creating chart for " + caliber);
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws IOException, CsvValidationException {
        
        String[] urls = {"https://ammoseek.com/ammo/9mm-luger?sh=lowest&ca=brass", "https://ammoseek.com/ammo/223-remington/-rifle-55grains?ca=brass&sh=lowest", "https://ammoseek.com/ammo/12-gauge?sl=2%203%2F4&sh=lowest"};
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yy");
        String d = dateFormat.format(new Date());

        for (int i = 0; i < urls.length; i++) {
            

            if(urls[i].contains("9mm-luger")) {
                BufferedWriter out = new BufferedWriter(new FileWriter("src/data/Test.csv", true));
                out.newLine();
                out.write("9mm," + d +","+ String.format("%.2f", getAveragePrice(urls[i])) + ",brass" );
                out.flush();
                out.close();
            } else if (urls[i].contains("223-remington")) {
                BufferedWriter out = new BufferedWriter(new FileWriter("src/data/Test.csv", true));
                out.newLine();
                out.write(".223rem," + d +","+ String.format("%.2f", getAveragePrice(urls[i])) + ",brass" );
                out.flush();
                out.close();
            } else if (urls[i].contains("12-gauge")) {
                BufferedWriter out = new BufferedWriter(new FileWriter("src/data/Test.csv", true));
                out.newLine();
                out.write("12-gauge," + d +","+ String.format("%.2f", getAveragePrice(urls[i])) + ",2.75" );
                out.flush();
                out.close();
            } else {
                BufferedWriter out = new BufferedWriter(new FileWriter("src/data/Test.csv", true));
                out.newLine();
                out.write("0," + "0," + "0," + "0");
                out.flush();
                out.close();
            }
        }

        readCSV("src/data/Test.csv");
        createCharts();
    }
}