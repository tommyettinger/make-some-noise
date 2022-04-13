package make.some.noise.gdx;

import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

/**
 * "Perlin Worms" for more than Perlin noise, in 2D.
 * Implementation closely derived from <a href="https://github.com/SunnyValleyStudio/Perlin-Worms-Unity-tutorial/blob/main/Scripts/PerlinWorm.cs">This
 * Unity tutorial</a>, which is MIT-licensed.
 */
public class Worm2D {
    private Vector3 direction;
    private Vector2 position;
    private Vector2 converge;
    private final Vector2 temp = new Vector2();
    private final Quaternion tq = new Quaternion();
    private Noise noise;
    private boolean moveToConverge = false;
    private float weight = 0.6f;

    public boolean isMoveToConverge() {
        return moveToConverge;
    }

    public void setMoveToConverge(boolean moveToConverge) {
        this.moveToConverge = moveToConverge;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = Math.min(Math.max(weight, 0.5f), 0.9f);
    }

    public Worm2D(Noise noise, Vector2 position) {
        this.position = position;
        this.noise = noise;
    }

    public Worm2D(Noise noise, Vector2 position, Vector2 convergencePoint) {
        this.position = position;
        this.converge = convergencePoint;
        moveToConverge = true;
        this.noise = noise;
    }


    public Vector2 moveToConverge()
    {
        Vector3 direction = getNoiseDirection();
        temp.set(converge).sub(position).nor();
        temp.scl(weight).add(direction.x * (1f - weight), direction.y * (1f - weight)).nor();
        position.add(temp);
        return position;
    }

    public Vector2 move()
    {
        getNoiseDirection();
        position.add(direction.x, direction.y);
        return position;
    }

    private Vector3 getNoiseDirection()
    {
        float noise = this.noise.getConfiguredNoise(position.x, position.y) * 90;
        direction = tq.setFromAxis(Vector3.X, noise).transform(direction).nor();
        return direction;
    }

    public Array<Vector2> moveLength(Array<Vector2> buffer, int length)
    {

        for (int i = 0; i < length; i++) {
            if (moveToConverge)
            {
                Vector2 result = moveToConverge().cpy();
                buffer.add(result);
                if (converge.dst2(result) < 1) {
                    break;
                }
            }
            else {
                buffer.add(move().cpy());
            }


        }
        if (moveToConverge)
        {
            while (converge.dst2(position) > 1)
            {
                weight = 0.9f;
                Vector2 result = moveToConverge().cpy();
                buffer.add(result);
                if (converge.dst2(result) < 1)
                {
                    break;
                }
            }
        }

        return buffer;
    }
}
