# Multi-Layer Neural Network Implementation

import numpy as np

class Linear:
  def forward(self, x): return x
  def backward(self, dy): return dy

class ReLU:
  ''' ReLU nonlinearity '''
  def forward(self, x):
    y=np.maximum(0, x)
    self.x=x
    return y

  def backward(self, dy):
    dx = (self.x>0)*dy
    return dx


class Logistic:
  ''' Logistic nonlinearity'''
  def forward(self, z):
    y = 1 / (1 + np.exp(-z))
    self.y = y
    return y

  def backward(self, dy):
    return self.y * (1 - self.y)


class Layer:
  ''' One neural layer '''

  def __init__(self, nInputs, nOutputs, NL):
    ''' nInputs : number of inputs
        nOutputs: number of outputs
        NL : ReLU, Logistic, or softmax objects
    '''
    self.nInputs=nInputs
    self.nOutputs=nOutputs
    self.W=np.zeros((nOutputs, nInputs))
    self.b=np.zeros((nOutputs, 1))
    self.dW=np.zeros((nOutputs, nInputs))
    self.db=np.zeros((nOutputs, 1))
    self.dZ=None
    self.NL=NL()

  def setRandomWeights(self, M=0.1):
    ''' set random uniform weights of max size M '''
    self.W=np.random.rand(self.nOutputs, self.nInputs)*M
    self.b=np.random.rand(self.nOutputs, 1)*M

  def copyWeightsFrom(self, src):
    ''' copy weight from another layer object of the same config '''
    np.copyto(self.W, src.W)
    np.copyto(self.b, src.b)

  def doForward(self, _input, remember=True):
    Z=self.W.dot(_input)+self.b
    _output = self.NL.forward(Z)
    if remember==True:
      self.Z=Z
      self.input=_input
    return _output

  def doBackward(self, dOutput):
    self.dZ=self.NL.backward(dOutput)
    self.dW=self.dZ@self.input.T
    self.db=np.sum(self.dZ, axis=1, keepdims=True)
    dInput=self.W.T@self.dZ
    return dInput

  def updateWeights(self, eta):
    self.W -= eta*self.dW
    self.b -= eta*self.db
    # breakpoint()

class NeuralNetwork:
  ''' a neural network '''

  def __init__(self, nInputs, layers, M=1e-1):
    ''' layers=( (n_neurons, NL), ... )
    '''
    if not isinstance(layers, list) or \
       not isinstance(layers[0], tuple) or \
       not isinstance(layers[0][0], int) or \
       not isinstance(layers[0][0], int):
      raise ValueError('layers must be a list of (nNeuron, NL) tuples')

    self.nLayers=len(layers)
    self.A=[None]*(self.nLayers+1)  # input + all activations
    self.dA=[None]*(self.nLayers+1) # input + all activations
    self.layers=[ Layer(nInputs, layers[0][0], layers[0][1]) ]
    for l in range(1,self.nLayers):
      self.layers.append( Layer(layers[l-1][0], layers[l][0], layers[l][1]) )
    self.setRandomWeights(M)

  def setRandomWeights(self, M=1e-1):
    for l in range(0,self.nLayers):
      self.layers[l].setRandomWeights(M)

  def copyWeightsFrom(self, src):
    for l in range(0,self.nLayers):
      self.layers[l].copyWeightsFrom(src.layers[l])

  def doForward(self, _input, remember=True):
    self.A[0]=_input  # A = activations
    for l in range(self.nLayers):
      self.A[l+1]=self.layers[l].doForward(self.A[l], remember)
    return self.A[self.nLayers]

  def predict(self, _input): return self.doForward(_input, False)

  def doBackward(self, dOutput):
    self.dA[self.nLayers] = dOutput
    for l in range(self.nLayers,0,-1):
      self.dA[l-1]=self.layers[l-1].doBackward(self.dA[l])
    return self.dA[0]

  def updateWeights(self, eta):
    for l in range(self.nLayers):
      self.layers[l].updateWeights(eta)

  def print(self, want=['W', 'dW', 'b', 'db', 'Z', 'dZ', 'Input'
        'Output', 'dInput', 'dOutput']):
    for l in range(self.nLayers):
      Map={'W':self.layers[l].W,
        'dW': self.layers[l].dW,
        'b': self.layers[l].b,
        'db': self.layers[l].db,
        'Z ': self.layers[l].Z,
        'dZ': self.layers[l].dZ,
        'Input': self.A[l],
        'Output': self.A[l+1],
        'dInput': self.dA[l],
        'dOutput': self.dA[l+1]}
      print('======== Layer %d =========' % l)
      for k in want:
        print('                           '+k + ':\n', Map[k])

  def save(self, filename):
    import pickle
    with open(filename, 'wb') as f:
      pickle.dump(self, f, pickle.HIGHEST_PROTOCOL)

  def load(self, filename):
    import os.path
    import pickle
    if os.path.isfile(filename):
      with open(filename, 'rb') as f:
        nn=pickle.load(f)
      self.__dict__.update(nn.__dict__)

class ObjectiveFunction:
  def crossEntropyLogitForward(self, logits, y):
    ''' Cross entropy between [log(p_1), ..., log(p_n)]+C, and
        [y_1, ..., y_n]. The former vector is called logits in Tensorflow.
        It can be obtained by just W*x+b, without any nonlinearity.
        Input logits and y are both n by m, where n is the number of classes
        and m is the number of data points.
    '''
    a = logits.max(axis=0)  # log-sum-exp trick
    logp = (logits-a)-np.log( np.sum(np.exp(logits-a), axis=0))
    J = -np.sum(logp*y) / y.shape[1]
    self.logp = logp
    return J

  def crossEntropyLogitBackward(self, y):
    dz = np.exp(self.logp)-y
    m=y.shape[1]
    dz*=1/m
    return dz

  def logisticForward(self, logits, y):
    ''' logit is w*x+b, one scalar for each data point.
        Input logits is 1 by m, where m is the number of points.
        Input y is 2 by m, first row y_0, and second row y_1.
    '''
    logits = np.vstack ((np.zeros((1, logits.size)), logits)) # padding 0 on top
    # return self.crossEntropyLogitForward(logits, y)
    return self.crossEntropyLogitForward(logits, np.vstack( (1-y,y) ))


  def logisticBackward(self, y):
    dz=self.crossEntropyLogitBackward( np.vstack( (1-y,y) ) )
    return dz[1,None]

  def leastSquaresForward(self, yhat, y):
    ''' Least squares cost function -- forward. Input yhat and y
        are both 1 by m, where m is the number of data points.
    '''
    self.diff = yhat-y
    return np.sum(self.diff*self.diff)/len(y)

  def leastSquaresBackward(self, y):
    ''' Least squares cost function -- backward. Uses the stored
        self.diff from the leastSquaresForward function.
    '''
    return 2*self.diff/len(y)

  def __init__(self, name):
    if name=='crossEntropyLogit':
      self.doForward=self.crossEntropyLogitForward
      self.doBackward=self.crossEntropyLogitBackward
    elif name=='logistic':
      self.doForward=self.logisticForward
      self.doBackward=self.logisticBackward
