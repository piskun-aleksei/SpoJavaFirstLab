/**
 * Created by Brotorias on 29.10.2016.
 */
public class СокетХуёкет {
    private int Скорость;
    private String КЧАУ;
    public СокетХуёкет(int Скорость, String КЧАУ){
        this.Скорость = Скорость;
        this.КЧАУ = КЧАУ;
    }

    public int гдеСкорость() {
        return Скорость;
    }

    public String аеКЧАУ() {
        return КЧАУ;
    }
}
