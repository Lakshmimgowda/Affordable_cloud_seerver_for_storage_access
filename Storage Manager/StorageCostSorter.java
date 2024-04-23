import java.util.Comparator;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Lohith
 */
public class StorageCostSorter implements Comparator {

    public int compare(Object o1, Object o2) {

        CloudClient c1 = (CloudClient)o1;
        CloudClient c2 = (CloudClient)o2;

        if (c1.scost>c2.scost)
        {
            return 1;
        }
        return -1;


    }

}
